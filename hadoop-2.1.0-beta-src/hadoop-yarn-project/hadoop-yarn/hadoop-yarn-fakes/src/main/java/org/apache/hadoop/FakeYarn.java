package org.apache.hadoop;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.util.Timer;
import java.util.Vector;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.PeriodicStatsAccumulator;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.server.api.ResourceTracker;
import org.apache.hadoop.yarn.server.api.ServerRMProxy;
import org.apache.hadoop.yarn.server.resourcemanager.MockNM;

/**
 * A YARN implementation that does no real work but fakes working against a real
 * RM.
 * 
 */
public class FakeYarn {
    public static void main(String[] args) {
        try {
            Configuration conf = new YarnConfiguration();
            System.out
                    .println(conf
                            .getInt(YarnConfiguration.RM_SCHEDULER_MINIMUM_ALLOCATION_MB,
                                    YarnConfiguration.DEFAULT_RM_SCHEDULER_MINIMUM_ALLOCATION_MB));
            Path yarnSitePath = new Path(
                    "/usr/local/hadoop/etc/hadoop/yarn-site.xml");

            conf.addResource(yarnSitePath);
            ResourceTracker resourceTracker;

            resourceTracker = ServerRMProxy.createRMProxy(conf,
                    ResourceTracker.class);
            Timer timer = new Timer();
            Vector<FakeNMContainerManager> cms = new Vector<FakeNMContainerManager>();
            // Start a bunch of NMs.
            for (int i = 0; i < 10; ++i) {
                MockNM nm = new MockNM("localhost:" + (12000 + i), 2 << 10,
                        resourceTracker);
                nm.registerNode();
                // Send periodic heart beats.
                timer.schedule(new FakeNMHeartBeatTask(nm), 0, conf.getLong(
                        YarnConfiguration.RM_NM_HEARTBEAT_INTERVAL_MS, 60000));
                cms.add(new FakeNMContainerManager(new InetSocketAddress(
                        "localhost", 12000 + i), nm));
                cms.lastElement().init(conf);
                cms.lastElement().start();
            }
            // Submit a bunch of applications.
            FakeClient client = new FakeClient(conf);
            client.submitApplication();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        // Also, with a specified random distribution fail.
        // We also have to bring up a fake container manager for each NM.
    }
}