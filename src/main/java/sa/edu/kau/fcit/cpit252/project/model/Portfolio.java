package sa.edu.kau.fcit.cpit252.project.model;

import java.util.ArrayList;
import java.util.List;

public class Portfolio implements PortfolioComponent {
    private String name;
    private List<PortfolioComponent> components = new ArrayList<>();

    public Portfolio(String name) {
        this.name = name;
    }

    public void add(PortfolioComponent component) {
        components.add(component);
    }

    public void remove(PortfolioComponent component) {
        components.remove(component);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public double getValue() {
        double total = 0;

        for(PortfolioComponent component : components){
           total += component.getValue();
        }
        return total;
    }

    @Override
    public void display(String indent) {
        System.out.println(indent + "Portfolio: " + name + " | Total value " + getValue());
        for (PortfolioComponent c : components){
            c.display(indent + "  ");
        }

    }
}
