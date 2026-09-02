package cn.demo.asset;

import cn.demo.NovaBootApplication;
import cn.demo.asset.log.FluentBitConfigService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

//@SpringBootTest(classes = NovaBootApplication.class)
public class TestApp {
//
//    @Resource
//    private FluentBitConfigService fluentBitConfigService;

//    @Test
//    public void test() {
//        String fbt = """
//                service:
//                  flush: 1
//                  log_level: info
//
//                pipeline:
//                  inputs:
//                    - name: random
//
//                  outputs:
//                    - name: stdout
//                      match: '*'
//                      format: 'json_lines'
//                """;
//        fluentBitConfigService.saveInput();
//    }

    @Test
    public void t1() {
        System.out.println(hnt(7, '左', '右', '中'));
    }

    int i = 1;

    public int hnt(int n, char form, char to, char other) {
        if (n == 0) return 0;
        int a = hnt(n - 1, form, other, to);
        System.out.println(i + ":" + form + "-->" + to);
        i++;
        int b = hnt(n - 1, other, to, form);
        return a + b + 1;
    }
/**
 fn hnt(n, form,to,other) {
 if (n == 0) return 0;
 let a = hnt(n - 1, form, other, to);
 console.log(form + "-->" + to);
 let b = hnt(n - 1, other, to, form);
 return a + b + 1;
 }
 */
}
