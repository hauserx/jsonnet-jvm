import jsonnetjvm.runtime.*;
import java.util.function.Supplier;

public class Example {

    // local Person(name='Alice') = { ... }
    public static Val person(Val name) {
        // Handle default parameter
        final Val finalName = (name != null) ? name : new JString("Alice");
        
        JObject obj = new JObject();
        
        // name: name
        obj.addField("name", () -> finalName);
        
        // welcome: 'Hello ' + name + '!'
        obj.addField("welcome", () -> new JString("Hello " + finalName.asString() + "!"));
        
        return obj;
    }

    public static void main(String[] args) {
        JObject root = new JObject();
        
        // person1: Person()
        root.addField("person1", new Thunk(() -> person(null)));
        
        // person2: Person('Bob')
        root.addField("person2", new Thunk(() -> person(new JString("Bob"))));
        
        System.out.println(root.toJson());
    }
}
