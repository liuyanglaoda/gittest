package com.szyciov.driver.mapper;/**
 * Created by admin on 2017/8/25.
 */

import com.szyciov.util.GsonUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Map;

/**
 * @author XZF
 * @Title: AccountMapperTest
 * @Package com.szyciov.driver.mapper
 * @Description
 * @date 2017/8/25 11:52
 * @Copyrigth 版权所有 (C) 2017 广州讯心信息科技有限公司.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:/applicationContext.xml"})
public class AccountMapperTest {

    @Autowired
    private AccountMapper accountMapper;

    @Test
    public void testPeopleVehicleRalation(){
        Map<String, String> stringStringMap = accountMapper.peopleVehicleRalation("0514D77F-A2E0-4766-B0A9-3D2697C02CCE");
        System.out.println(GsonUtil.toJson(stringStringMap));
    }
}
