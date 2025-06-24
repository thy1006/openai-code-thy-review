package plus.gaga.middleware;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import java.util.Arrays;
import java.util.List;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ApiTest {

    @Test
    public void test() throws Exception {
        System.out.println(Integer.parseInt("11"));
        System.out.println(Integer.parseInt("11"));
        System.out.println(Integer.parseInt("11"));
//        System.out.println(Integer.parseInt("deepseek"+"sk-68eef7cef00444c09089350f96117030"));
        List<Integer> numbers = Arrays.asList(5, 3, 8, 1, 2);
        numbers.sort((a, b)-> Integer.compare(a,b));
        Animal dog = new Animal(){
            @Override
            public void speak() {
                System.out.println("Dog barks");
            }
        };
        dog.speak();
        System.out.println(1111);

    }
}
