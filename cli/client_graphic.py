import networkx as nx 
import numpy as np 
import matplotlib.pyplot as plt 
import glob 
import pylab
import os 



# def graph_render(H,p1,f,r0="",r1="",p2='graphs'):
#     '''
#     Graph Rendering Function
#     Renders single graph given
    
#     Input:
#     ------
#     H: (NetworkX Graph Object): The graph for which the community needs to be detected
#     p1: (str): The master path for the directories to be created in
#     f: (str): The name of the file, used in the name of the graph's rendered image as well
#     r0: (int): The minima of the range in consideration for which the network is being constructed/analyzed
#     r1: (int): The maxima of the range in consideration for which the network is being constructed/analyzed
#     p2: (str): Optional. Default='community': The name of the directory in which the result from this subroutine is to be stored
#     Output:
#     -------
#     returns: null
#     Stores the graph in the path given
    
#     '''
#     p5=""
#     path=p1+p2+'/'
#     if not os.path.exists(path):
#         os.makedirs(path)
    
#     labels={}    
#     for j in range(0,len(H)):
#         #labels[H[i].nodes()[j]]=r'Res#'+str(H[i].nodes()[j])+':'+lab2[dg1[H[i].nodes()[j]]]
#         #labels[H[i].nodes()[j]]=str(H[i].nodes()[j])+':'+lab3[lab2[dg1[H[i].nodes()[j]]]]
#         #labels[H[i].nodes()[j]]=lab3[lab2[dg1[H[i].nodes()[j]]]]
#         # import pdb; pdb.set_trace()
#         labels[list(H.nodes())[j]]=j
#     pos=nx.fruchterman_reingold_layout(H,k=0.15,iterations=10) 
#     #pos=nx.spring_layout(H,k=0.15,iterations=100)
#     val_map={72:0.3,
#              74:0.3,
#              372:0.3,
#              373:0.3,
#              374:0.3}
#     values = [val_map.get(node, 0.25) for node in H.nodes()]
#     #pos=nx.shell_layout(H[i])
#     #pos=nx.random_layout(H[i])
#     #nx.draw(H[i],with_labels=True)
#     pylab.figure(1,figsize=(23,23))
#     #nx.draw_networkx_nodes(H[i],pos,node_size=1000,node_color=values)
#     #nx.draw_networkx_nodes(H[i],pos,node_size=values*4000,node_color=values)
#     nx.draw_networkx_nodes(H,pos,nodelist=[x for x in val_map.keys() if x in H.nodes()],node_size=2000,node_color="#FF0000")
#     nx.draw_networkx_nodes(H,pos,nodelist=list(set(H.nodes())-set(val_map.keys())),node_size=800,node_color="#A0CBE2")
#     #nx.draw_networkx_nodes(H,pos,nodelist=list(set(H.nodes())-set(val_map.keys())),node_size=800,node_color="#FF0000")
#     nx.draw_networkx_edges(H,pos)
#     nx.draw_networkx_labels(H,pos,labels,font_color='orange',font_weight='bold',font_size=20)
#     pylab.axis('off')
#     pylab.savefig(path+f.split('/')[-1]+'_'+str(p5)+'_'+str(r0)+'_'+str(r1)+'.png')
#     #pylab.savefig(path+f.split('/')[-1]+'_'+str(p5)+'_type4.png')
#     pylab.close()
#     ##plt.axis('off')
#     #plt.show()
#     ##plt.savefig(path+f[i].split('\\')[1]+'_type4.png',bbox_inches='tight',figsize=[1000,1000])
#     #plt.show()
#     ##plt.close()


def graph_render(H,p1,f,r0="",r1="",p2='graphs'):
    '''
    Graph Rendering Function
    Renders single graph given
    
    Input:
    ------
    H: (NetworkX Graph Object): The graph for which the community needs to be detected
    p1: (str): The master path for the directories to be created in
    f: (str): The name of the file, used in the name of the graph's rendered image as well
    r0: (int): The minima of the range in consideration for which the network is being constructed/analyzed
    r1: (int): The maxima of the range in consideration for which the network is being constructed/analyzed
    p2: (str): Optional. Default='community': The name of the directory in which the result from this subroutine is to be stored
    Output:
    -------
    returns: null
    Stores the graph in the path given
    
    '''
    p5=""
    path=p1+p2+'/'
    G=H
    if not os.path.exists(path):
        os.makedirs(path)

    # val_map = {'A': 1.0,
    #                'D': 0.5714285714285714,
    #                           'H': 0.0}
    val_map={}

    values = [val_map.get(node, 0.45) for node in G.nodes()]
    edge_labels=dict([((u,v,),d['weight'])
                    for u,v,d in G.edges(data=True)])
    node_labels = {node:node for node in G.nodes()}
    # red_edges = [('C','D'),('D','A')]
    # edge_colors = ['black' if not edge in red_edges else 'red' for edge in G.edges()]
    edge_colors=[]

    # pos=nx.spring_layout(G)
    pos=nx.fruchterman_reingold_layout(G,k=0.5,iterations=10)
    nx.draw_networkx_edge_labels(G,pos,edge_labels=edge_labels)
    nx.draw_networkx_labels(G, pos, labels=node_labels)
    # nx.draw(G,pos, node_color = values, node_size=1500,edge_color=edge_colors,edge_cmap=plt.cm.Reds)
    nx.draw(G,pos, node_color = 'red', node_size=1500,edge_cmap=plt.cm.Reds)
    # pylab.show()
    pylab.axis('off')
    pylab.show()
    pylab.savefig(
        path
        + f.split('/')[-1]
        + '_'
        + p5
        + '_'
        + str(r0)
        + '_'
        + str(r1)
        + '.png'
    )

    #pylab.savefig(path+f.split('/')[-1]+'_'+str(p5)+'_type4.png')
    pylab.close()
    ##plt.axis('off')
    #plt.show()
    ##plt.savefig(path+f[i].split('\\')[1]+'_type4.png',bbox_inches='tight',figsize=[1000,1000])
    #plt.show()
    ##plt.close()


LEVEL=3     # Level to query at
ID="A"      # ID to be searched

fnames = glob.glob(f'join_{LEVEL}/*')
arrays = [np.loadtxt(f, delimiter=',', dtype=str) for f in fnames]
join_file = np.concatenate(arrays)

itemindex = np.where(join_file==ID)[0]

if len(itemindex):
    G=nx.DiGraph()
    for item in itemindex:
        individual_transactions=np.array_split(join_file[item],LEVEL)
        for x in individual_transactions:
            if G.has_edge(x[1],x[2]):
                G[x[1]][x[2]]['weight']+=float(x[3])
            else:
                G.add_edge(x[1],x[2],weight=float(x[3]))
        # [G.add_edge(x[1],x[2],weight=float(x[3])) for x in individual_transactions]
    # plotting logic
    graph_render(G,'', 'graph_1')

else:
    print("No transactions found")
