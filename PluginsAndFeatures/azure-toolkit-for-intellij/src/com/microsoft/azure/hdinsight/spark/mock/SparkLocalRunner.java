/*
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 */

package com.microsoft.azure.hdinsight.spark.mock;


import mockit.Invocation;
import mockit.Mock;
import mockit.MockUp;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

public class SparkLocalRunner {
    public static void main(String[] args) {
        SparkLocalRunner localRunner = new SparkLocalRunner();

        localRunner.setUp();
        localRunner.runJobMain(Arrays.asList(args));
    }

    private void runJobMain(List<String> args) {

        System.out.println("HADOOP_HOME: " + System.getenv("HADOOP_HOME"));
        System.out.println("Working directory: " + System.getProperty("user.dir"));

        String jobClassName = args.get(0);

        try {
            final Class<?> jobClass = Class.forName(jobClassName);

            System.out.println("Run Spark Job: " + jobClass.getName());

            final Method jobMain = jobClass.getMethod("main", String[].class);

            final Object[] jobArgs = new Object[]{ args.subList(1, args.size()).toArray(new String[0]) };
            jobMain.invoke(null, jobArgs);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

    }

    private void setUp() {

        new MockUp<FileSystem>() {

            @Mock
            public Class getFileSystemClass(Invocation invocation, String scheme, Configuration conf) {
                return MockDfs.class;
            }

            @Mock
            public void checkPath(Path path) {}
        };

        System.setProperty("spark.master", "local[2]");
//        System.setProperty("HADOOP_USER_NAME", System.getProperty("user.name"));
    }
}