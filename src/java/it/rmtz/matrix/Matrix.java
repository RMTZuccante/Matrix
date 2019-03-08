package it.rmtz.matrix;

import it.rmtz.camera.Camera;
import it.rmtz.matrix.Cell.Victim;
import it.rmtz.matrix.SerialConnector.Color;

import java.awt.*;

import static it.rmtz.matrix.SerialConnector.*;

public class Matrix {
    private final static byte NORTH = 0, EAST = 1, SOUTH = 2, WEST = 3;
    private final Camera left, right;
    private byte direction;
    private SerialConnector connector;
    private int maxWallDist;
    private Cell start, actual;
    private float bodyTemp;
    private Point coords;

    private Step firstStep = new Step();

    public Matrix(SerialConnector connector, Camera left, Camera right, int maxWallDist, float bodyTemp) {
        this.left = left;
        this.right = right;
        this.connector = connector;
        this.maxWallDist = maxWallDist;
        this.bodyTemp = bodyTemp;
        direction = NORTH;
        coords = new Point(0, 0);
    }

    public void start() {
        while (!connector.handShake()) {
            System.err.println("Handshake failed!");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                System.exit(-1);
            }
        }

        connector.setDebug((byte) 0);

        start = actual = new Cell();

        while (true) {
            boolean victimfound = actual.victim != Victim.NONE;
            inspectCell();
            Direction dir = nextDirection();
            if (dir != null) {
                switch (dir) {
                    case BACK:
                        System.out.println("Go back:");
                        System.out.println("\tRotate right");
                        connector.rotate(90);
                        inspectCell();
                        System.out.println("\tRotate right again");
                        connector.rotate(90);
                        break;
                    case LEFT:
                        System.out.println("Go left");
                        connector.rotate(-90);
                        inspectCell();
                        break;
                    case RIGHT:
                        System.out.println("Go right");
                        connector.rotate(90);
                        inspectCell();
                        break;
                    case FRONT:
                        System.out.println("Do not rotate");
                        break;

                }
                if (actual.victim != Victim.NONE && !victimfound) {
                    int packages = 0;

                    if (actual.victim == Victim.HEAT || actual.victim == Victim.S) {
                        packages = 1;
                    } else if (actual.victim == Victim.H) {
                        packages = 2;
                    }
                    connector.victim(packages);
                }


                System.out.println("Go straight");
                direction = getNewCardinalDirection(direction, dir);

                go(true);
                int goret = connector.go();
                if (goret == GOBLACK) {
                    actual.black = true;
                    firstStep.next = null;
                    go(false);
                } else {
                    if (goret == GOOBSTACLE) actual.weight = 10;
                    if (goret == GORISE) actual.weight = 20;
                }
            } else {
                System.out.println("Finished! MISSION COMPLETED!");
                break;
            }
        }
    }

    private void inspectCell() {
        short[] distances = connector.getDistances();

        if (distances[DFRONTL] > maxWallDist) { //TODO bottle
            addFrontCell();
        }
        if (distances[DLEFT] > maxWallDist) {
            addLeftCell();
        }
        if (distances[DRIGHT] > maxWallDist) {
            addRightCell();
        }
        if (distances[DBACK] > maxWallDist) {
            addBackCell();
        }

        actual.mirror = isMirror();
        if (isVictim()) actual.victim = Victim.HEAT;
        actual.visited = true;
    }

    private void go(boolean forward) {
        if (forward) {
            actual = getCellByCardinalDirection(actual, direction);
        } else {
            actual = getCellByCardinalDirection(actual, getNewCardinalDirection(direction, Direction.BACK));
        }
        int dir = forward ? direction : getNewCardinalDirection(direction, Direction.BACK);
        coords = getPointByDirection(coords, dir);
    }

    private Point getPointByDirection(Point sp, int dir) {
        Point p = new Point(sp.x, sp.y);
        switch (dir) {
            case NORTH:
                p.y++;
                break;
            case SOUTH:
                p.y--;
            case EAST:
                p.x++;
            case WEST:
                p.x--;
        }
        return p;
    }

    private boolean isMirror() {
        Color color = connector.getColor();
        // TODO improve detections rules
        return (color.getBlue() < color.getGreen()) && color.getRed() < color.getGreen();
    }

    private boolean isVictim() {
        float[] temps = connector.getTemps();
        // TODO improve detections rules
        return temps[TLEFT] > bodyTemp || temps[TRIGHT] > bodyTemp;
    }

    private Direction nextDirection() {
        Direction dir = null;
        boolean gotNewDir = false;
        if (firstStep.next == null) {
            if (pathFinding(actual, firstStep, direction) == -1) {
                if (actual != start) {
                    System.err.println("No more cells, ALL VISITED!, going back to home");
                    start.visited = false;
                    if (pathFinding(actual, firstStep, direction) == -1) {
                        System.err.println("Failed pathfinding to home! ERROR!");
                    } else gotNewDir = true;
                }
            } else gotNewDir = true;
        } else gotNewDir = true;

        if (gotNewDir) {
            dir = firstStep.next.direction;
            firstStep.next = firstStep.next.next;
        }
        return dir;
    }

    private int pathFinding(Cell cell, Step prev, byte direction) {
        int weight = -1;
        if (cell != null && !cell.considered && !cell.black) {
            if (cell.visited) {
                cell.considered = true;

                Step[] steps = new Step[]{new Step(Direction.FRONT), new Step(Direction.RIGHT), new Step(Direction.LEFT), new Step(Direction.BACK)};
                byte[] cardinals = new byte[]{direction, getNewCardinalDirection(direction, Direction.RIGHT), getNewCardinalDirection(direction, Direction.LEFT), getNewCardinalDirection(direction, Direction.BACK)};
                int[] weights = new int[]{0, 1, 1, 2};
                int pos = -1;

                for (int i = 0; i < 4; i++) {
                    int tempw = pathFinding(getCellByCardinalDirection(cell, cardinals[i]), steps[i], cardinals[i]);
                    if (tempw != -1) {
                        tempw += weights[i];
                        if ((tempw - weights[i]) == 0 || weight == -1 || tempw < weight) {
                            weight = tempw;
                            pos = i;
                        }
                    }
                }

                if (weight != -1) {
                    weight += cell.weight;
                    prev.next = steps[pos];
                }
                cell.considered = false;
            } else {
                weight = 0;
                prev.next = null;
            }
        }
        return weight;
    }

    private Cell getCellByCardinalDirection(Cell cell, byte dir) {
        Cell c;
        if (dir == NORTH) c = cell.north;
        else if (dir == SOUTH) c = cell.south;
        else if (dir == EAST) c = cell.east;
        else c = cell.west;
        return c;
    }

    private byte getNewCardinalDirection(byte start, Direction rot) {
        if (rot == Direction.LEFT) {
            start--;
        } else if (rot == Direction.RIGHT) {
            start++;
        } else if (rot == Direction.BACK) {
            start += 2;
        }
        return (byte) Math.floorMod(start, 4);
    }

    private void addFrontCell() {
        addCell(NORTH, SOUTH, EAST, WEST);
    }

    private void addRightCell() {
        addCell(WEST, EAST, NORTH, SOUTH);
    }

    private void addLeftCell() {
        addCell(EAST, WEST, SOUTH, NORTH);
    }

    private void addBackCell() {
        addCell(SOUTH, NORTH, WEST, EAST);
    }

    private void addCell(byte south, byte north, byte west, byte east) {
        if (direction == south && actual.north == null) {
            actual.north = new Cell();
            actual.north.south = actual;
        } else if (direction == north && actual.south == null) {
            actual.south = new Cell();
            actual.south.north = actual;
        } else if (direction == west && actual.east == null) {
            actual.east = new Cell();
            actual.east.west = actual;
        } else if (direction == east && actual.west == null) {
            actual.west = new Cell();
            actual.west.east = actual;
        }
    }

    enum Direction {
        LEFT,
        RIGHT,
        FRONT,
        BACK
    }
}
