package jdbi.doc;

import org.jdbi.v3.core.config.JdbiConfig;

// tag::exampleConfig[]
public class ExampleConfig implements JdbiConfig<ExampleConfig> {

    private String color;
    private int number;

    public ExampleConfig() {
        color = "purple";
        number = 42;
    }

    private ExampleConfig(ExampleConfig other) {
        this.color = other.color;
        this.number = other.number;
    }

    public ExampleConfig setColor(String color) {
        this.color = color;
        return this;
    }

    public String getColor() {
        return color;
    }

    public ExampleConfig setNumber(int number) {
        this.number = number;
        return this;
    }

    public int getNumber() {
        return number;
    }

    @Override
    public ExampleConfig createCopy() {
        return new ExampleConfig(this);
    }

}
// end::exampleConfig[]
