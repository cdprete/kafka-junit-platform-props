import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashSet;

import static java.util.Collections.list;

/**
 * @author Cosimo Damiano Prete
 */
class ClassPathTest {
    @Test
    void printClassPath() throws IOException {
        var loader = getClass().getClassLoader();
        System.out.println();
        System.out.println("loader = " + loader);
        System.out.println("loader.parent = " + loader.getParent());
        System.out.println("loader.parent.parent = " + loader.getParent().getParent());
        System.out.println();
        System.out.println("single: " + loader.getResource("junit-platform.properties"));
        System.out.println();
        var urls = list(loader.getResources("junit-platform.properties"));
        urls.forEach(url -> System.out.println("multi: " + url));
        System.out.println();
        var distinct = new HashSet<>(urls);
        distinct.forEach(url -> System.out.println("distinct: " + url));
    }
}
