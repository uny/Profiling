import edu.uci.ics.jung.algorithms.layout.*;
import edu.uci.ics.jung.graph.*;
import edu.uci.ics.jung.graph.event.GraphEvent.Vertex;
import edu.uci.ics.jung.visualization.*;
import edu.uci.ics.jung.visualization.decorators.*;  
import edu.uci.ics.jung.visualization.renderers.Renderer.VertexLabel.*;
import java.awt.*;
import javax.swing.JFrame;

public class Jung {
	public static final int WIDTH = 500;
    public static final int HEIGHT = 500;
    public static final int MARGIN = 50;

    public static void main(String[] args) {
        new Jung().run();
    }
    public void run(){
        showGraph(makeGraph(5));
    }
    Graph makeGraph(int size) {
        Graph g = new SparseGraph();
        for(int i=0; i< size; i++) g.addVertex(i);
        int num = 0;
        for(int i=0; i< size; i++)
             for(int j=0; j< i; j++) {
                g.addEdge(""+num++, i, j);
        }
        return g;
    }
    void showGraph(Graph g){
        Layout layout = new CircleLayout(g);
        layout.setSize(new Dimension(WIDTH,HEIGHT));
        BasicVisualizationServer vv = new BasicVisualizationServer(layout);
        vv.setPreferredSize(new Dimension(WIDTH+MARGIN,HEIGHT+MARGIN));
        vv.getRenderContext().setVertexLabelTransformer(new ToStringLabeller());
        vv.getRenderContext().setEdgeLabelTransformer(new ToStringLabeller());
        vv.getRenderContext().setEdgeShapeTransformer(new EdgeShape.Line());
        vv.getRenderer().getVertexLabelRenderer().setPosition(Position.CNTR);
        JFrame frame = new JFrame("Jung Test");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(vv);
        frame.pack();
        frame.setVisible(true);
    }
}
