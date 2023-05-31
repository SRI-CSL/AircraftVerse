import math
from typing import Tuple

import torch
from torch import nn, Tensor
import torch.nn.functional as F
from torch.nn import TransformerEncoder, TransformerEncoderLayer
from torch.utils.data import dataset
from torch.nn.utils.rnn import pack_padded_sequence, pad_packed_sequence


from torch.utils.data import Dataset, DataLoader

class Data(Dataset):
    """Simple Dataset"""

    def __init__(self, x_train, y_train, mask, transform=None):
        """
        Args:
            transform (callable, optional): Optional transform to be applied
                on a sample.
        """
        
        self.x_train = x_train
        self.y_train = y_train
        self.mask = mask
        self.transform = transform

    def __len__(self):
        return len(self.y_train)

    def __getitem__(self, idx):
        if torch.is_tensor(idx):
            idx = idx.tolist()

        sample = (self.x_train[idx], self.y_train[idx], self.mask[idx])

        if self.transform:
            sample = self.transform(sample)

        return sample

def prepare_sequence_data(data_path, spec, batch_size = 512 ,batch_size_val = 512, frac_train = 0.7, frac_val = 0.1):
    assert frac_train + frac_val < 1.
    
    scale_1 = None # min/mean
    scale_2 = None #max/std
    dic = torch.load(data_path)

    seq_len_max = max([d.shape[0] for d in dic['X_norm']])
    X_norm = [dic['X_norm'][i] for i in range(len(dic['X_norm'])) if dic['X_norm'][i].shape[0] <= seq_len_max]
    if spec == 'airworthy':
        Y = torch.tensor([dic['airworthy'][i] for i in range(len(dic['airworthy'])) if dic['X_norm'][i].shape[0] <= seq_len_max])
    if spec == 'interference':
        Y = torch.tensor([dic['interference_list'][i] > 0 for i in range(len(dic['interference_list'])) if dic['X_norm'][i].shape[0] <= seq_len_max]).int()
    if spec == 'mass':
        X_norm = [dic['X_norm'][i] for i in range(len(dic['X_norm'])) if dic['X_norm'][i].shape[0] <= seq_len_max and dic['y'][i] < max_mass]
        Y = torch.tensor([dic['y'][i] for i in range(len(dic['airworthy'])) if dic['X_norm'][i].shape[0] <= seq_len_max and dic['y'][i] <max_mass])
        valid_ind = torch.logical_not(torch.isnan(Y)) # Remove Nan masses
        X_norm = [X_norm[i] for i in range(len(X_norm)) if valid_ind[i].item()]
        scale_1 = Y[valid_ind].mean(0)
        scale_2 = Y[valid_ind].std(0)
        Y = (Y[valid_ind] - scale_1)/scale_2
    if spec == 'dist':
        dist = [0. if value is None else value for value in dic['max_distance']]
        Y = torch.tensor([dist[i] for i in range(len(dic['max_distance'])) if dic['X_norm'][i].shape[0] <= seq_len_max])
        valid_ind = torch.logical_not(torch.isnan(Y)) # Remove Nan masses
        X_norm = [X_norm[i] for i in range(len(X_norm)) if valid_ind[i].item()]
        scale_1 = Y[valid_ind].min()
        scale_2 = Y[valid_ind].max()
        Y = (Y[valid_ind] + Y[valid_ind].min())/(scale_2 - scale_1)
    if spec == 'hover':
        dist = [0 if value is None else value for value in dic['hover_time']]
        Y = torch.tensor([dist[i] for i in range(len(dic['hover_time'])) if dic['X_norm'][i].shape[0] <= seq_len_max])
        valid_ind = torch.logical_not(torch.isnan(Y)) # Remove Nan masses
        X_norm = [X_norm[i] for i in range(len(X_norm)) if valid_ind[i].item()]
        scale_1 = Y[valid_ind].mean(0)
        scale_2 = Y[valid_ind].std(0)
        Y = (Y[valid_ind] - scale_1)/scale_2 

    X = torch.nn.utils.rnn.pad_sequence(X_norm).transpose(0,1) # padding sequences

    src_mask = torch.zeros(X.shape[0],seq_len_max).bool()
    for n, d in enumerate(X):
        src_mask[n] = (d.sum(-1) == 0)

    D = X.shape[-1]
    N = X.shape[0]
    SL = X.shape[-2] # sequence length

    N_train = int(frac_train * N) # default: 70 %
    N_val = int(frac_val * N) # default: 10 %

    indices = torch.randperm(N)

    Y_norm = Y.float()

    data_tr = Data(X[indices[:N_train]], Y_norm[indices[:N_train]], src_mask[indices[:N_train]])
    dataloader_tr = DataLoader(data_tr, batch_size=batch_size,
                            shuffle=True, num_workers=1)
    data_val = Data(X[indices[N_train:N_train+N_val]], Y_norm[indices[N_train:N_train+N_val]], src_mask[indices[N_train:N_train+N_val]])
    dataloader_val = DataLoader(data_val, batch_size=len(data_val),
                            shuffle=False, num_workers=1)
    data_test = Data(X[indices[N_train+N_val:]], Y_norm[indices[N_train+N_val:]], src_mask[indices[N_train+N_val:]])
    dataloader_test = DataLoader(data_test, batch_size=len(data_test),
                            shuffle=False, num_workers=1)
    
    return dataloader_tr, dataloader_val, dataloader_test, scale_1, scale_2

class TransformerModel(nn.Module):

    def __init__(self, d_model: int, nhead: int, d_hid: int,
                 nlayers: int, dropout: float = 0.01, D: int = 741,
                 D_out: int = 1): #dropout was 0.5
        super().__init__()
        
        self.D = D
        self.D_out = D_out
        self.model_type = 'Transformer'
        self.pos_encoder = PositionalEncoding(d_model, dropout)
        encoder_layers = TransformerEncoderLayer(d_model, nhead, d_hid, dropout)
        self.transformer_encoder = TransformerEncoder(encoder_layers, nlayers)
        self.encoder = nn.Linear(self.D, d_model)
        self.d_model = d_model
        self.decoder = nn.Linear(d_model, self.D_out)

        self.init_weights()

    def init_weights(self) -> None:
        initrange = 0.1
        self.encoder.weight.data.uniform_(-initrange, initrange)
        self.decoder.bias.data.zero_()
        self.decoder.weight.data.uniform_(-initrange, initrange)

    def forward(self, src: Tensor, src_mask: Tensor) -> Tensor:
        """
        Args:
            src: Tensor, shape [seq_len, batch_size]
            src_mask: Tensor, shape [seq_len, seq_len]

        Returns:
            output Tensor of shape [seq_len, batch_size, ntoken]
        """
#         Need to permute from B x SL x D to SL x B x D
        src = self.encoder(src.permute(1, 0, 2)) * math.sqrt(self.d_model)
        src = self.pos_encoder(src)
        output = self.transformer_encoder(src, src_key_padding_mask=src_mask)# #Don't use a mask here, we want to predict over whole sequence, src_mask)
        output = self.decoder(output[-1])
        return output

class PositionalEncoding(nn.Module):

    def __init__(self, d_model: int, dropout: float = 0.1, max_len: int = 5000):
        super().__init__()
        self.dropout = nn.Dropout(p=dropout)

        position = torch.arange(max_len).unsqueeze(1)
        div_term = torch.exp(torch.arange(0, d_model, 2) * (-math.log(10000.0) / d_model))
        pe = torch.zeros(max_len, 1, d_model)
        pe[:, 0, 0::2] = torch.sin(position * div_term)
        pe[:, 0, 1::2] = torch.cos(position * div_term)
        self.register_buffer('pe', pe)

    def forward(self, x: Tensor) -> Tensor:
        """
        Args:
            x: Tensor, shape [seq_len, batch_size, embedding_dim]
        """
        x = x + self.pe[:x.size(0)]
        return self.dropout(x)

def generate_square_subsequent_mask(sz: int) -> Tensor:
    """Generates an upper-triangular matrix of -inf, with zeros on diag."""
    return torch.triu(torch.ones(sz, sz) * float('-inf'), diagonal=1) 
    
class LSTM(nn.Module):

    def __init__(self, D, emsize, dimension=128):
        super(LSTM, self).__init__()

        self.embedding = nn.Linear(D, emsize)
        self.dimension = dimension
        self.lstm = nn.LSTM(input_size=emsize,
                            hidden_size=dimension,
                            num_layers=1,
                            batch_first=True,
                            bidirectional=True)
        self.drop = nn.Dropout(p=0.5)

        self.fc = nn.Linear(2*dimension, 1)

    def forward(self, text, text_len):

        text_emb = self.embedding(text)

        output, _ = self.lstm(text_emb)

        out_forward = output[range(len(output)), text_len - 1, :self.dimension]
        out_reverse = output[:, 0, self.dimension:]
        out_reduced = torch.cat((out_forward, out_reverse), 1)
        text_fea = self.drop(out_reduced)

        text_out = self.fc(text_fea)

        return text_out