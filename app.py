# Run this app with `python app.py` and
# visit http://127.0.0.1:8050/ in your web browser.

from dash import Dash, html, dcc
import plotly.express as px
import pandas as pd
import dash_bootstrap_components as dbc


import dash_vtk
from dash_vtk.utils import to_mesh_state

try:
    # VTK 9+
    from vtkmodules.vtkImagingCore import vtkRTAnalyticSource
except ImportError:
    # VTK =< 8
    from vtk.vtkImagingCore import vtkRTAnalyticSource

def read_stl(stl_file):
    txt_content = None
    with open(stl_file, 'r') as file:
      txt_content = file.read()

    content = dash_vtk.View([
        dash_vtk.GeometryRepresentation([
            dash_vtk.Reader(
                vtkClass="vtkSTLReader",
                parseAsText=txt_content,
            ),
        ]),
    ])
    return content

stl_file_35 = "./assets/datasets/design_35.stl"
des_35 = read_stl(stl_file_35)

stl_file_445 = "./assets/datasets/design_445.stl"
des_445 = read_stl(stl_file_445)

stl_file_1096 = "./assets/datasets/design_1096.stl"
des_1096 = read_stl(stl_file_1096)

# Themes: https://dash-bootstrap-components.opensource.faculty.ai/docs/themes/
app = Dash(__name__, title='UAV Designverse', external_stylesheets=[dbc.themes.DARKLY])
# DARKLY colors: https://github.com/thomaspark/bootswatch/blob/c143eda36a068054ebad9b4c80314c40f13c9c10/docs/5/darkly/_variables.scss
server = app.server

colors = {
    'background': '#222',
    'text': '#7FDBFF'
}

app.layout = html.Div(children=[
    html.Div(
        className="app-header",
        children=[
            html.Div('AircraftVerse', className="app-header--title")
        ],
        style={
            'color': colors['text']
              }
    ),

    html.A([
    html.Img(src='./assets/nusci.jpg', style={
        'display': 'inline-block',
        'position': 'absolute',
        'height': '60px',
        'top':'5px',
        'left':'10px'
        # "height": "3%"
    },),], href='https://nusci.csl.sri.com/'),

    html.Center([

    html.Div(children='We present AircraftVerse, a publicly available dataset with over 28,000 diverse set of air vehicle designs.',
    style={
        'textAlign': 'center',
        'color': colors['text'],
        'width': '80%',
        'padding': '32px 32px'
    }),



    # First graph

    html.Div(
    # style={"width": "100%", "height": "400px"},
    children=[des_35],
    style={
        'display': 'inline-block',
        'vertical-align': 'top',
        'width': '33%',
        "height": "200px",
        'padding': '8px 0px'
    },
    ),

    # Second graph
    html.Div(
    # style={"width": "100%", "height": "400px"},
    children=[des_445],
    style={
        'display': 'inline-block',
        'vertical-align': 'top',
        'width': '33%',
        "height": "200px",
        'padding': '8px 0px'
    },
    ),

    # Third graph
    html.Div(
    # style={"width": "100%", "height": "400px"},
    children=[des_1096],
    style={
        'display': 'inline-block',
        'vertical-align': 'top',
        'width': '33%',
        "height": "200px",
        'padding': '8px 0px'
    },
    ),

    html.Div(
    # style={"width": "100%", "height": "400px"},
    children=['Use the mouse to manipulate the UAV designs (STL Files) in the figures above.'],
    style={
        # 'display': 'inline-block',
        'vertical-align': 'top',
        'width': '80%',
        "font-weight": "bold",
        # "height": "200px",
        'padding': '8px 0px'
    },
    ),


    html.Div(children='Aircraft design encompasses different physics domains and, hence, multiple modalities of representation. The evaluation of these designs requires the use of scientific analytical and simulation models ranging from computer-aided design tools for structural and manufacturing analysis, computational fluid dynamics tools for drag and lift computation, battery models for energy estimation, and simulation models for flight control and dynamics. AircraftVerse contains over 28,000 diverse set of air vehicle designs - the largest corpus of designs with this level of complexity.  Each design comprises the following artifacts: a symbolic design tree describing topology, propulsion subsystem, battery subsystem, and other design details; a STandard for the Exchange of Product (STEP) model data; a 3D CAD design using Standard Tessellation Languages (STL);  a 3D point cloud for the shape of the design; and evaluation results from high fidelity state-of-the-art physics models that characterize performance metrics such as maximum flight distance and hover-time.  We also present baseline surrogate models that use different modalities of design representation to predict design performance metrics, which we provide as part of our dataset release.',
    style={
        'textAlign': 'justify',
        'color': colors['text'],
        'width': '80%',
        'padding': '32px 32px'
    }),

    html.A([html.Button('Dataset', id='btn-nclicks-1', n_clicks=0, className = "button button2"),], href = 'https://www.sri.com/'),
    html.A([html.Button('Paper', id='btn-nclicks-2', n_clicks=0, className = "button button2"),], href = 'https://www.sri.com/'),
    html.A([html.Button('Explore Designs', id='btn-nclicks-3', n_clicks=0, className = "button button2"),], href = 'https://www.sri.com/'),

    dcc.Markdown('''### Acknowledgements

This project was supported by DARPA under the Symbiotic
Design for Cyber-Physical Systems (SDCPS) with contract
FA8750-20-C-0002.
The views, opinions and/or findings expressed
are those of the author and should not be interpreted as
representing the official views or policies of the Department
of Defense or the U.S. Government.
This dataset was developed as a collaboration between 
researchers at SRI International, 
SwRI, Vanderbilt University, CMU and Purdue University''', style = {'textAlign': 'center',
'color': colors['text'], 'width': '80%'} )

    ]),

    ])




if __name__ == '__main__':
    app.run_server(debug=True)
