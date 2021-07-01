package de.oshgnacknak.gruphi;

import h07.graph.DirectedGraph;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Optional;

class GruphiFrame extends JFrame {

    private final DirectedGraph<Node, Double> graph;

    private final MazeGenerator<Node> mazeGenerator;
    private final Gruphi gruphi;

    private Node selected = null;
    private final Vector vel = new Vector(0, 0);
    private boolean running = true;

    GruphiFrame(Gruphi gruphi) {
        super("Gruphi - The Graph GUI - By Osh");

        this.gruphi = gruphi;
        this.graph = gruphi.getDirectedGraphFactory().createDirectedGraph();
        this.mazeGenerator = new MazeGenerator<>(graph, gruphi.getNeighbourPredicate());

        add(new Canvas(this::draw));
        pack();
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        var mouseListener = new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                switch (e.getButton()) {
                    case MouseEvent.BUTTON1: {
                        if (selected != null) {
                            var clicked = findClickedNode(e);
                            if (clicked.isPresent()) {
                                var n = clicked.get();
                                if (graph.getChildrenForNode(selected).contains(n)) {
                                    graph.disconnectNodes(selected, n);
                                } else {
                                    graph.connectNodes(selected, 1.0, n);
                                }
                            } else {
                                selected.pos.x = e.getX();
                                selected.pos.y = e.getY();
                            }
                        } else {
                            graph.addNode(new Node(e.getX(), e.getY()));
                        }
                    } break;

                    case MouseEvent.BUTTON3: {
                        if (selected != null) {
                            selected.color = Node.COLOR;
                            selected.radius = Node.RADIUS;
                            selected = null;
                        }
                        findClickedNode(e)
                            .ifPresent(n -> {
                                selected = n;
                                selected.radius *= 1.3;
                                selected.color = Color.RED;
                            });
                    } break;

                    default: break;
                }
            }
        };

        addMouseListener(mouseListener);
        addMouseMotionListener(mouseListener);

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_K:
                    case KeyEvent.VK_W:
                    case KeyEvent.VK_UP: {
                        vel.y = -gruphi.getVelocity();
                    } break;

                    case KeyEvent.VK_J:
                    case KeyEvent.VK_S:
                    case KeyEvent.VK_DOWN: {
                        vel.y = gruphi.getVelocity();
                    } break;

                    case KeyEvent.VK_H:
                    case KeyEvent.VK_A:
                    case KeyEvent.VK_LEFT: {
                        vel.x = -gruphi.getVelocity();
                    } break;

                    case KeyEvent.VK_L:
                    case KeyEvent.VK_D:
                    case KeyEvent.VK_RIGHT: {
                        vel.x = gruphi.getVelocity();
                    } break;

                    case KeyEvent.VK_X:
                    case KeyEvent.VK_DELETE:
                    case KeyEvent.VK_BACK_SPACE: {
                        if (selected != null) {
                            graph.removeNode(selected);
                            selected = null;
                        }
                    } break;

                    case KeyEvent.VK_Q:
                    case KeyEvent.VK_ESCAPE: {
                        running = false;
                    } break;

                    case KeyEvent.VK_C: {
                        clearGraph();
                    } break;

                    case KeyEvent.VK_M: {
                        if (selected != null) {
                            mazeGenerator.generate(selected);
                        }
                    } break;

                    case KeyEvent.VK_G: {
                        generateGrid();
                    } break;

                    default: break;
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_K:
                    case KeyEvent.VK_J:
                    case KeyEvent.VK_W:
                    case KeyEvent.VK_S:
                    case KeyEvent.VK_DOWN:
                    case KeyEvent.VK_UP: {
                        vel.y = 0;
                    } break;

                    case KeyEvent.VK_H:
                    case KeyEvent.VK_L:
                    case KeyEvent.VK_A:
                    case KeyEvent.VK_D:
                    case KeyEvent.VK_RIGHT:
                    case KeyEvent.VK_LEFT: {
                        vel.x = 0;
                    } break;

                    default: break;
                }
            }
        });
    }

    private void generateGrid() {
        clearGraph();

        var dist = gruphi.getGridSpacing();

        var rows = getHeight() / dist - 1;
        var cols = getWidth() / dist - 1;

        for (int x = 0; x < cols; x++) {
            for (int y = 0; y < rows; y++) {
                var n = new Node((x + 0.5) * dist, (y + 0.5) * dist);
                graph.addNode(n);
            }
        }
    }

    private void clearGraph() {
        selected = null;
        for (var node : graph.getAllNodes()) {
            graph.removeNode(node);
        }
    }

    private Optional<Node> findClickedNode(MouseEvent e) {
        return graph.getAllNodes()
            .stream()
            .filter(n ->
                n.inside(e.getX(), e.getY()))
            .findFirst();
    }

    private void draw(Drawable d) {
        d.fill(Color.BLACK);
        d.rect(0, 0, getWidth(), getHeight());

        d.strokeWeight(1);
        d.fill(Color.WHITE);
        for (var node : graph.getAllNodes()) {
            for (var child : graph.getChildrenForNode(node)) {
                d.line(node.pos.x, node.pos.y, child.pos.x, child.pos.y);

                var v = child.pos
                    .copy()
                    .sub(node.pos);
                var r = 4;
                var p = v.copy()
                    .setMag(v.mag() - child.radius - r)
                    .add(node.pos);

                d.rotated(v.angle(), p.x, p.y, () ->
                    d.triangle(
                        p.x+r, p.y,
                        p.x-r, p.y-r,
                        p.x-r, p.y+r));
            }
        }

        for (var node : graph.getAllNodes()) {
            d.fill(node.color);
            d.ellipse(node.pos.x, node.pos.y, node.radius * 2, node.radius * 2);
        }
    }

    public void updateLoop() {
        var last = System.currentTimeMillis();
        var acc = 0;

        while (running) {
            while (acc > gruphi.getFrameDelay()) {
                update();
                acc -= gruphi.getFrameDelay();
            }
            repaint();

            var current = System.currentTimeMillis();
            acc += current - last;
            last = current;
        }
    }

    private void update() {
        if (selected != null) {
            selected.pos.add(vel);
        }
    }
}