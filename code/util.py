import matplotlib.pyplot as plt
import numpy as np
import itertools
from stl import mesh
from mpl_toolkits import mplot3d
import os
import json

def plot_confusion_matrix(cm,
                          target_names,
                          title='Confusion matrix',
                          cmap=None,
                          normalize=True):
    """
    given a sklearn confusion matrix (cm), make a nice plot

    Arguments
    ---------
    cm:           confusion matrix from sklearn.metrics.confusion_matrix

    target_names: given classification classes such as [0, 1, 2]
                  the class names, for example: ['high', 'medium', 'low']

    title:        the text to display at the top of the matrix

    cmap:         the gradient of the values displayed from matplotlib.pyplot.cm
                  see http://matplotlib.org/examples/color/colormaps_reference.html
                  plt.get_cmap('jet') or plt.cm.Blues

    normalize:    If False, plot the raw numbers
                  If True, plot the proportions

    Usage
    -----
    plot_confusion_matrix(cm           = cm,                  # confusion matrix created by
                                                              # sklearn.metrics.confusion_matrix
                          normalize    = True,                # show proportions
                          target_names = y_labels_vals,       # list of names of the classes
                          title        = best_estimator_name) # title of graph

    Citiation
    ---------
    http://scikit-learn.org/stable/auto_examples/model_selection/plot_confusion_matrix.html

    """


    accuracy = np.trace(cm) / float(np.sum(cm))
    misclass = 1 - accuracy

    if cmap is None:
        cmap = plt.get_cmap('Blues')

    plt.figure(figsize=(5,3))

    if target_names is not None:
        tick_marks = np.arange(len(target_names))
        plt.xticks(tick_marks, target_names, rotation=45)
        plt.yticks(tick_marks, target_names)

    if normalize:
        cm = cm.astype('float') / cm.sum(axis=1)[:, np.newaxis]
    
    plt.imshow(cm, interpolation='nearest', cmap=cmap)
    plt.title(title)
    thresh = cm.max() / 1.5 if normalize else cm.max() / 2
    for i, j in itertools.product(range(cm.shape[0]), range(cm.shape[1])):
        if normalize:
            plt.text(j, i, "{:0.4f}".format(cm[i, j]),
                     horizontalalignment="center",
                     color="white" if cm[i, j] > thresh else "black")
        else:
            plt.text(j, i, "{:,}".format(cm[i, j]),
                     horizontalalignment="center",
                     color="white" if cm[i, j] > thresh else "black")


    plt.tight_layout()
    plt.ylabel('True label')
    plt.xlabel('Predicted label')
    print('accuracy={:0.4f}; misclass={:0.4f}'.format(accuracy, misclass))
    
    plt.show()
    
    
def plot_stl(path = None, design = None):
    # Create a new plot
    figure = plt.figure(figsize=(14,8))
    axes = figure.add_subplot(111, projection="3d")#, elev=elev, azim=azim)

    # Load the STL files and add the vectors to the plot
    your_mesh = mesh.Mesh.from_file(os.path.join(os.path.join(path,design), 'cadfile.stl'))
    
    your_mesh.vectors[:,:,-1] = -your_mesh.vectors[:,:,-1]
    
    axes.add_collection3d(mplot3d.art3d.Poly3DCollection(your_mesh.vectors, alpha = 0.3))

    # Auto scale to the mesh size
    scale = your_mesh.points.flatten()
    axes.auto_scale_xyz(scale, scale, scale)

    # Show the plot to the screen
    # plt.axis('off')
    plt.show()
    # return your_mesh.vectors

def plot_pointCloud(path, design):
    pc = np.load(os.path.join(os.path.join(path,design), 'pointCloud.npy'))
    
    fig = plt.figure(figsize=(12,7))
    ax = fig.add_subplot(projection='3d')
    img = ax.scatter(pc[:,0], pc[:,1], -pc[:,2], 'o', alpha = 0.9, s = 0.5, color = 'C0', rasterized = True)
    # fig.colorbar(img)

    ax.set_xlabel('X')
    ax.set_ylabel('Y')
    ax.set_zlabel('Z')
    
    ax.set_box_aspect([np.ptp(a) for a in [pc[:,0], pc[:,1], -pc[:,2]]])

    ax.view_init(elev=10, azim=90)

    plt.axis('off')

    plt.tight_layout()

    plt.show()    


col_names = ['distance',
 'flight_time',
 'pitch_angle',
 'max_uc',
 'thrust',
 'lift',
 'drag',
 'current',
 'total_power',
 'frac_amp',
 'frac_pow',
 'frac_current']

col_names_read = ['Distance (m)',
 'Flight Time (s)',
 'Pitch (deg.)',
 'Max Control', # Max uc is the max control variable uc (or throttle) 0-1.
 'Thrust (N)',
 'Lift (N)',
 'Drag (N)',
 'Current (A)',
 'Power (W)',
 r'$I_{mot}/I_{max}$', # Frac amp is the ratio of the current in the motor divided by the maximum allowed current, then the maximum is taken over all the motors
 r'$P_{mot}/P_{max}$', # Frac pow is the same thing for motor power
 r'$I_{bat}/I_{max}$' #Frac current is the battery current divided by maximum allowable battery current, taken over all the batteries.
                 ]


def plot_trim_stats(vel_array, names, fs = 14, figsize=(10, 2)):
    
    indices = [col_names.index(name) for name in names]
    
    fig, axs = plt.subplots(1, len(indices), figsize=figsize, constrained_layout=True) 
    # sharey = True)
    for i, ax in enumerate(axs.flat):
        ax.set_title(f'{col_names_read[indices[i]]}', fontsize = fs)
        ax.plot(vel_array[:,indices[i]], 'o', ls='-', ms=4)
        ax.grid()
        # if i == 0:
        ax.set_xlabel('Trim (m/s)', fontsize = fs)
        ax.tick_params(labelsize=fs * 0.9)
    # plt.show()
    return plt
    
    
def collect_design_parts(path, design):
    lowlevel = open(os.path.join(os.path.join(path,design), 'design_low_level.json'))
    lowlevel_json = json.load(lowlevel)
    lowlevel_json_print = json.dumps(lowlevel_json, indent=4)
    lowlevel.close()
    parts = [part['component_type'] for part in lowlevel_json['components']]
    return parts

def count(string, Y):
    count_list = []
    for y in Y:
        count_list.append(sum([1. for i in y if string in i]))
    return count_list


def hist(part_count, ylabel = None, xlabel = "Number of UAV Designs", title = None, fs = 14, figsize = (8,4)):
    
    x, v = np.unique(part_count, return_counts=True)

    x_pos = [i for i, _ in enumerate(x)]
    plt.style.use('default')
    plt.figure(figsize=figsize)
    plt.tick_params(axis='both', which='both', labelsize=.9 * fs)
    plt.grid(axis = 'y')
    plt.bar(x_pos, v, width = 0.3, color='C0')
    plt.xlabel(xlabel, fontsize = fs * 1.8)
    plt.ylabel(ylabel, fontsize = fs * 1.8)
    plt.title(title, fontsize = fs)

    plt.xticks(x_pos, np.int32(x))
    
    def addlabels(ax,x,y):
        for i in range(len(x)):
            ax.text(i, y[i] + max(v) * 0.02, y[i], ha = 'center', fontsize = 0.8*fs)
    
    addlabels(plt, x_pos, v)
    plt.ylim(0,max(v) * 1.11)
    plt.tight_layout()
    return plt
    
def scatter(x_scatter, y_scatter, ylabel = None, xlabel = None, title = None, fs = 14, figsize = (8,4), xlim = [0,2000]):
    plt.style.use('default')
    plt.figure(figsize=figsize)
    plt.tick_params(axis='both', which='both', labelsize=.9 * fs)

    plt.plot(x_scatter, y_scatter, '.', markersize = 2., color = 'C0', rasterized = True, alpha = 0.4)
    plt.xlabel(xlabel, fontsize = fs * 1.8)
    # plt.ylabel(ylabel, fontsize = fs * 1.8)
    
    # Get current axes
    ax = plt.gca()

    # Set y-axis label and get its position
    y_label = ax.set_ylabel(ylabel, fontsize = fs * 1.8)
    y_label_pos = y_label.get_position()

    # Move the y-axis label downward
    y_label.set_position((y_label_pos[0], y_label_pos[1] - 0.1))

    
    plt.title(title, fontsize = fs)
    plt.grid()
    plt.tight_layout()
    plt.xlim(xlim)

    # plt.savefig('./Images/mass_hover_scatter.pdf')
    # plt.show()
    return plt

def subplot_hist_scatter(hist_data, scatter_data, hist_labels=None, scatter_labels=None, titles=None, fs=14, figsize=(15,10), xlim=[0,2000]):
    fig, ax = plt.subplots(2, 3, figsize=figsize)
    
    i = -1
    j = -1
    c = 0
    for l, data in enumerate(hist_data):
        if c % 3 == 0:
            i += 1
            j = 0
        else:
            j += 1
                
        x, v = np.unique(data, return_counts=True)
        x_pos = [i for i, _ in enumerate(x)]
        ax[i, j].grid(axis = 'y')
        ax[i, j].bar(x_pos, v, width = 0.3, color='C0')
        ax[i, j].set_xlabel(hist_labels[l][0] if hist_labels else "Designs", fontsize = fs * 1.8)
        ax[i, j].set_ylabel(hist_labels[l][1] if hist_labels else None, fontsize = fs * 1.8)
        ax[i, j].set_title(titles[i] if titles else None, fontsize = fs)
        ax[i, j].set_xticks(x_pos, np.int32(x), rotation = 90)
        ax[i, j].tick_params(axis='x', which='both', labelsize=.9 * fs)
        ax[i, j].tick_params(axis='y', which='both', labelsize=.9 * fs)
        
        for k in range(len(x)):
            ax[i, j].text(k, v[k] + max(v) * 0.02, v[k], ha = 'center', fontsize = 0.7*fs)
        ax[i, j].set_ylim(0,max(v) * 1.11)
        c+=1
    for k, data in enumerate(scatter_data):
        if c % 3 == 0:
            i += 1
            j = 0
        else:
            j += 1
        ax[i, j].plot(data[0], data[1], '.', markersize = 2., color = 'C0', rasterized = True, alpha = 0.4)
        ax[i, j].set_xlabel(scatter_labels[k][0] if scatter_labels else None, fontsize = fs * 1.8)
        ax[i, j].set_ylabel(scatter_labels[k][1] if scatter_labels else None, fontsize = fs * 1.8)
        ax[i, j].set_title(titles[i+2] if titles else None, fontsize = fs)
        ax[i, j].set_xlim(xlim[k])
        ax[i, j].grid()
        ax[i, j].tick_params(axis='x', which='both', labelsize=.9 * fs)
        ax[i, j].tick_params(axis='y', which='both', labelsize=.9 * fs)

        
        c+=1
    
    fig.align_ylabels()
    fig.align_xlabels()
    fig.tight_layout()

    return plt