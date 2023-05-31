import argparse
import torch
import os
import json
import pickle

'''
python build_transformer_data.py --input ../data --save-path ../data/transformer_data 
'''


# From component_library.py: NuSymCORE/swri/src/misc/component_library.py
# These are problematic components that we do not include
blacklist = [
    "TurnigyReceiver1500mAh4_8V",
    "Turnigynano-tech2000mAh2040C",
    "Turnigy_nano_tech_2000mAh_20_40C",
    "Turnigynanotech3000mAh_2040C",
    "Turnigynano-tech3000mAh2040C",
    "TurnigReceiver1500mAh6_0V",
    "TurnigyReceiver1500mAh6_0V",
    "TurnigyReceiver1500mAh6.0V",
    "TurnigyGraphene4000mAh3S5C"
]

blacklist_keys = [
    "arm1length",
    "arm2length"
]

replacement_blacklist_keys = ["arm1Length", "arm2Length"]

design_seq_start = 2 # i.e. miss out generator version and "name" in design sequence


def encoding(all_words):
    unique_keys   = list(set(all_words))
    encoding_dict_keys = {}
    numbers = []
    i = 0
    for k in unique_keys:
        if isinstance(k, str):
            encoding_dict_keys[k] = i
            i+=1
    return encoding_dict_keys

a_file = open("../data/corpus_dic", "rb")
corpus_dic = pickle.load(a_file)
a_file.close()

Motor = torch.stack([torch.tensor(list(corpus_dic['Motor'][m].values())) for m in list(corpus_dic['Motor'].keys())])
Battery = torch.stack([torch.tensor(list(corpus_dic['Battery'][esc].values())) for esc in list(corpus_dic['Battery'].keys())])
Propeller = torch.stack([torch.tensor(list(corpus_dic['Propeller'][p].values())) for p in list(corpus_dic['Propeller'].keys())])

USE_DICT = True # Build input from all components and not just what was in the data

# Main code
if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Run a generated design through the pipeline.')
    parser.add_argument('--input', type=str, help='input directory containing design_tree.json, or subdirectories containing those')
    parser.add_argument('--save-path', type=str, help='save location, e.g. "../data/saved"')
    parser.add_argument('--model-data', type=str, help='If you have an encoding already decided, pass the path to the dictionary here', default = 'NULL')
    parser.add_argument('--test', help="If testing, we do not have labels so must build data set differently", action="store_true")
    args = parser.parse_args()
    
    path = args.input
    save_path = args.save_path
    encoding_path = args.model_data
    design_folders = os.listdir(path)
    
    # Labels available:
    if not args.test:
        design_sequence_list = []
        hover_time = []
        max_speed = []
        max_distance = []
        airworthy_list = []
        mass_list = []
        interference_list = []
        folder_list = []
        
        for folder in design_folders:
            if os.path.exists(os.path.join(os.path.join(path, folder),'design_seq.json')):
            
                with open(os.path.join(os.path.join(path, folder),'design_seq.json'), "r") as f:
                    design_sequence_list.append(json.load(f))
                try:
                    with open(os.path.join(os.path.join(path, folder),'output.json'), "r") as f:

                        d = json.load(f)

                        mass_list.append(d['Mass'])
                        interference_list.append(d['Interferences'])
                        hover_time.append(d['Hover_Time'])
                        max_speed.append(d['Max_Speed'])
                        max_distance.append(d['Max_Distance'])

                        if d['Hover_Time']:
                            airworthy_list.append(1)
                        else:
                            airworthy_list.append(0)

                        folder_list.append(folder)
                except FileNotFoundError:
                    design_sequence_list.pop()
            else:
                print('File ', os.path.join(os.path.join(path, folder),'design_seq.json'), ' does not exist')
    else:
        design_sequence_list = []
        folder_list = []

        for folder in design_folders:
            with open(os.path.join(os.path.join(path, folder),'design_seq.json'), "r") as f:
                design_sequence_list.append(json.load(f))
                folder_list.append(folder)

    print('Working with {} designs'.format(len(folder_list)))
    lengths = []
    for design in design_sequence_list:
        lengths.append(len(design)) 
    
    full_keys = [list(item.keys())[0] for design in design_sequence_list for item in design[design_seq_start:]]
    # filter out black list for generator keys:
    full_keys = [full_keys[i] for i in range(len(full_keys)) if full_keys[i] not in blacklist_keys]
    
    
    if encoding_path == 'NULL':
        if USE_DICT:
            # Use all components in dictionary:
            full_values = []
            compTypes = ['Motor', 'Battery', 'Propeller']
            for compType in compTypes:
                full_values.extend(list([key for key in list(corpus_dic[compType].keys())]))    
        #     Extend to those used in the data set
            # Just use those in the data set:
            full_values_from_data = [list(item.values())[0] for design in design_sequence_list for item in design[design_seq_start:]]
            full_values = full_values + full_values_from_data
        else:
            # Just use those in the data set:
            full_values = [list(item.values())[0] for design in design_sequence_list for item in design[design_seq_start:]]
        
        # filter out black list:
        full_values = [full_values[i] for i in range(len(full_values)) if full_values[i] not in blacklist]
        # Build dictionaries
        encoding_dict_keys = encoding(full_keys)
        encoding_dict_values = encoding(full_values)
        
        # Add explicit float token to values:
        encoding_dict_values['Value'] = len(encoding_dict_values.values())
        
    else:
        dic = torch.load(encoding_path)
        encoding_dict_keys = dic['encoding_dict_keys']
        encoding_dict_values = dic['encoding_dict_values']
            
    
    #### Collect all the floats
    
    float_list = []
    
    for design in design_sequence_list:
        for i, d in enumerate(design[design_seq_start:]):
            item = list(d.items())
            k = item[0][0]
            v = item[0][1]
            if isinstance(v, str):
                pass
            else:
                float_list.append(k)  
    
    float_names = set(float_list)
    float_dict = dict(zip(float_names, [ [] for _ in range(len(float_names)) ]))
    
    for design in design_sequence_list:
        for i, d in enumerate(design[design_seq_start:]):
            item = list(d.items())
            k = item[0][0]
            v = item[0][1]
            if isinstance(v, str):
                pass
            else:
                for name in float_names:
                    if k == name:
                        float_dict[name].append(v)

    for name in float_names:
        float_dict[name] = torch.tensor(float_dict[name]) 
        
    for replace_name, blacklist_name in zip(replacement_blacklist_keys, blacklist_keys):
        if blacklist_name in float_names:
            blacklist_values = float_dict.pop(blacklist_name)
            float_dict[replace_name] = torch.cat([float_dict[replace_name], blacklist_values])
    
    float_names = list(float_dict.keys())
    
    ### Build data set
    
    # [type, value, float]
    # Build data one hot
    K_length = len(encoding_dict_keys)
    V_length = len(encoding_dict_values)
    Motor_length = Motor.shape[-1]
    Battery_length = Battery.shape[-1]
    Propeller_length = Propeller.shape[-1]

    comp_attr_length = Motor_length + Battery_length + Propeller_length

    data = []
    data_norm = []
    for design in design_sequence_list:
        tensor = torch.zeros(len(design[design_seq_start:]), len(encoding_dict_keys) + len(encoding_dict_values) + 1 + comp_attr_length)
        tensor_norm = torch.zeros(len(design[design_seq_start:]), len(encoding_dict_keys) + len(encoding_dict_values) + 1 + comp_attr_length)
        for i, d in enumerate(design[design_seq_start:]):
            item = list(d.items())
            k = item[0][0]
            v = item[0][1]
            
            if k in blacklist_keys:
                ind = blacklist_keys.index(k)
                k = replacement_blacklist_keys[ind]
            
            if isinstance(v, str):
                tensor[i, encoding_dict_keys[k]] = 1
                tensor[i, K_length + encoding_dict_values[v]] = 1
                tensor[i,-1] = 0

                tensor_norm[i, encoding_dict_keys[k]] = 1
                tensor_norm[i, K_length + encoding_dict_values[v]] = 1
                tensor_norm[i,-1] = 0
                if k == 'motorType':
                    start = K_length + V_length                     
                    stop = K_length + V_length + Motor_length
                    attributes = list(corpus_dic['Motor'][v].values())
                    tensor[i,start:stop] = torch.tensor(attributes)
                    tensor_norm[i,start:stop] = (torch.tensor(attributes) - Motor.mean(0))/Motor.std(0)
                if k == 'batteryType':
                    start = K_length + V_length + Motor_length
                    stop = K_length + V_length + Motor_length + Battery_length
                    attributes = list(corpus_dic['Battery'][v].values())
                    tensor[i,start:stop] = torch.tensor(attributes)
                    tensor_norm[i,start:stop] = (torch.tensor(attributes) - Battery.mean(0))/Battery.std(0)
                if k == 'propType':
                    start = K_length + V_length + Motor_length + Battery_length
                    stop = K_length + V_length + Motor_length + Battery_length + Propeller_length
                    try:
                        attributes = list(corpus_dic['Propeller'][v].values())
                    except KeyError:
                        attributes = list(corpus_dic['Propeller'][v + 'E'].values())
                    tensor[i,start:stop] = torch.tensor(attributes)
                    tensor_norm[i,start:stop] = (torch.tensor(attributes) - Propeller.mean(0))/Propeller.std(0)
            else:
                tensor[i, encoding_dict_keys[k]] = 1
                tensor[i, K_length + encoding_dict_values['Value']] = 1
                tensor_norm[i, encoding_dict_keys[k]] = 1
                tensor_norm[i, K_length + encoding_dict_values['Value']] = 1
                
                tensor[i,-1] = v
                
                for name in float_names:
                    if name == k:
                        if isinstance(float_dict[name], torch.BoolTensor):
                            tensor_norm[i,-1] = float(v)
                        elif float_dict[name].std() == 0:
                            tensor_norm[i,-1] = v - float_dict[name].mean()
                        else:
                            tensor_norm[i,-1] = (v - float_dict[name].mean())/float_dict[name].std()

        data.append(tensor)
        data_norm.append(tensor_norm)
    
    if args.test:
        data_set = {'X': data, 'X_norm': data_norm,
                     'encoding_dict_keys': encoding_dict_keys,
                     'encoding_dict_values': encoding_dict_values,
                     'norm_dict': float_dict, 'path':path, 'folders': folder_list}
        torch.save(data_set, save_path)
        
    else: 
        data_set = {'X': data, 'X_norm': data_norm, 'y': mass_list, 'airworthy': airworthy_list, 'hover_time': hover_time, 'max_speed':max_speed, 'max_distance': max_distance, 'interference_list': interference_list, 'encoding_dict_keys': encoding_dict_keys, 'encoding_dict_values': encoding_dict_values, 'norm_dict': float_dict, 'path':path, 'folders': folder_list}
        torch.save(data_set, save_path)