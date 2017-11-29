package com.szyciov.driver.util;

import com.supervision.dto.SupervisionDto;
import com.supervision.enums.CommandEnum;
import com.supervision.enums.InterfaceType;
import com.szyciov.util.JUnit4ClassRunner;
import com.szyciov.util.SupervisionMessageUtil;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;


/**
 * Created by lzw on 2017/8/24.
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@Transactional
public class SupervisionMessageUtilTest {
    @Autowired
    private SupervisionMessageUtil supervisionMessageUtil;
    @Test
//    @Ignore
    public void send() throws Exception {
        SupervisionDto supervisionDto=new SupervisionDto();
        supervisionDto.setInterfaceType(InterfaceType.BASIC);
        supervisionDto.setCommandEnum(CommandEnum.CompanyServiceOrgan);
        supervisionDto.setTimestamp(System.currentTimeMillis());
        Map<String, String> map = new HashMap<>();
        map.put("flag", "1");
        map.put("serviceorganId", "C292CB21-816D-4E63-99CA-CE4E572CBC79");
        supervisionDto.setDataMap(map);
        supervisionMessageUtil.send(supervisionDto);
    }

}