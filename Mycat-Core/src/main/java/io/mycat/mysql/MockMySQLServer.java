/*
 * Copyright (c) 2016, OpenCloudDB/MyCAT and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software;Designed and Developed mainly by many Chinese
 * opensource volunteers. you can redistribute it and/or modify it under the
 * terms of the GNU General Public License version 2 only, as published by the
 * Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Any questions about this component can be directed to it's project Web address
 * https://code.google.com/p/opencloudb/.
 *
 */
 
package io.mycat.mysql;
 
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mycat.engine.SQLEngineCtx;
import io.mycat.mysql.back.MySQLDataSource;
import io.mycat.mysql.back.PhysicalDBNode;
import io.mycat.mysql.back.PhysicalDBPool;
import io.mycat.mysql.back.PhysicalDatasource;
import io.mycat.net2.ExecutorUtil;
import io.mycat.net2.NIOAcceptor;
import io.mycat.net2.NIOConnector;
import io.mycat.net2.NIOReactorPool;
import io.mycat.net2.NameableExecutor;
import io.mycat.net2.NamebleScheduledExecutor;
import io.mycat.net2.NetSystem;
import io.mycat.net2.SharedBufferPool;
import io.mycat.net2.SystemConfig;
import io.mycat.net2.mysql.config.DBHostConfig;
/**
 * 
 * @author wuzhihui
 *
 */
public class MockMySQLServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(MockMySQLServer.class);
    public static final int PORT = 8066;

    public static final String MOCK_HOSTNAME = "host1";

    public static final String MOCK_SCHEMA = "mysql";

    public static final Map<String, PhysicalDBNode> mockDBNodes;

    static {
        mockDBNodes = new HashMap<>();
        DBHostConfig config = new DBHostConfig("host1", "127.0.0.1", 3306, "mysql", "root", "123456");
        config.setMaxCon(10);
        PhysicalDatasource dbSource = new MySQLDataSource(config, false);
        PhysicalDBPool dbPool = new PhysicalDBPool("host1", new PhysicalDatasource[] { dbSource }, new HashMap<>());
        PhysicalDBNode dbNode = new PhysicalDBNode("host1", "mysql", dbPool);
        mockDBNodes.put("host1", dbNode);
    }

    public static void main(String[] args) throws IOException {
        // Business Executor ，用来执行那些耗时的任务
        NameableExecutor businessExecutor = ExecutorUtil.create("BusinessExecutor", 10);
        // 定时器Executor，用来执行定时任务
        NamebleScheduledExecutor timerExecutor = ExecutorUtil.createSheduledExecute("Timer", 5);

        SharedBufferPool sharedPool = new SharedBufferPool(1024 * 1024 * 100, 1024);
        new NetSystem(sharedPool, businessExecutor, timerExecutor);
        // Reactor pool
        NIOReactorPool reactorPool = new NIOReactorPool("Reactor Pool", 5, sharedPool);
       
        NIOConnector connector = new NIOConnector("NIOConnector", reactorPool);
        connector.start();
        NetSystem.getInstance().setConnector(connector);
        NetSystem.getInstance().setNetConfig(new SystemConfig());
        //SQLEngineCtx enginCtx=new SQLEngineCtx();
       
       
         MySQLFrontendConnectionFactory frontFactory = new MySQLFrontendConnectionFactory();
        NIOAcceptor server = new NIOAcceptor("Server", "127.0.0.1", PORT, frontFactory, reactorPool);
        server.start();
        // server started
        LOGGER.info(server.getName() + " is started and listening on " + server.getPort());
    }
}
