package it.rmtz.matrix;

public class Step {
    Matrix.Direction direction;
    Step next;

    Step() {

    }

    Step(Matrix.Direction direction) {
        this.direction = direction;
    }

    Step(Matrix.Direction d, Step next) {
        direction = d;
        this.next = next;
    }
}
