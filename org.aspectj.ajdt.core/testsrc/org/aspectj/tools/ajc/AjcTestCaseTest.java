/* *******************************************************************
 * Copyright (c) 2004 IBM Corporation
 * All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 
 * which accompanies this distribution and is available at 
 * http://www.eclipse.org/legal/epl-v10.html 
 *  
 * Contributors: 
 *     Adrian Colyer, 
 * ******************************************************************/
package org.aspectj.tools.ajc;

import org.aspectj.util.FileUtil;

import java.io.File;
import java.io.IOException;

/**
 * @author colyer
 *         Exercise the features of the AjcTestCase class and check they do as
 *         expected
 */
public class AjcTestCaseTest extends AjcTestCase {

  public void test() throws IOException {
    final File baseDir = new File("E:\\temp\\test3");
    final String[] args = new String[]{
        "-showWeaveInfo",
        "-g",
        "-encoding", "UTF-8",
        "-source", "1.7",
        "-verbose",
        "-d",
        "F:\\dropbox\\Dropbox\\codereview\\out\\production\\common",
        "-classpath", "F:\\dropbox\\Dropbox\\codereview\\tools.jar;F:\\repos\\a1\\trunk\\vendors\\Java\\jdk-1.7-win-64\\jre\\lib\\charsets.jar;F:\\repos\\a1\\trunk\\vendors\\Java\\jdk-1.7-win-64\\jre\\lib\\deploy.jar;F:\\repos\\a1\\trunk\\vendors\\Java\\jdk-1.7-win-64\\jre\\lib\\javaws.jar;F:\\repos\\a1\\trunk\\vendors\\Java\\jdk-1.7-win-64\\jre\\lib\\jce.jar;F:\\repos\\a1\\trunk\\vendors\\Java\\jdk-1.7-win-64\\jre\\lib\\jfr.jar;F:\\repos\\a1\\trunk\\vendors\\Java\\jdk-1.7-win-64\\jre\\lib\\jfxrt.jar;F:\\repos\\a1\\trunk\\vendors\\Java\\jdk-1.7-win-64\\jre\\lib\\jsse.jar;F:\\repos\\a1\\trunk\\vendors\\Java\\jdk-1.7-win-64\\jre\\lib\\management-agent.jar;F:\\repos\\a1\\trunk\\vendors\\Java\\jdk-1.7-win-64\\jre\\lib\\plugin.jar;F:\\repos\\a1\\trunk\\vendors\\Java\\jdk-1.7-win-64\\jre\\lib\\resources.jar;F:\\repos\\a1\\trunk\\vendors\\Java\\jdk-1.7-win-64\\jre\\lib\\rt.jar;F:\\repos\\a1\\trunk\\vendors\\Java\\jdk-1.7-win-64\\jre\\lib\\ext\\access-bridge-64.jar;F:\\repos\\a1\\trunk\\vendors\\Java\\jdk-1.7-win-64\\jre\\lib\\ext\\dnsns.jar;F:\\repos\\a1\\trunk\\vendors\\Java\\jdk-1.7-win-64\\jre\\lib\\ext\\jaccess.jar;F:\\repos\\a1\\trunk\\vendors\\Java\\jdk-1.7-win-64\\jre\\lib\\ext\\localedata.jar;F:\\repos\\a1\\trunk\\vendors\\Java\\jdk-1.7-win-64\\jre\\lib\\ext\\sunec.jar;F:\\repos\\a1\\trunk\\vendors\\Java\\jdk-1.7-win-64\\jre\\lib\\ext\\sunjce_provider.jar;F:\\repos\\a1\\trunk\\vendors\\Java\\jdk-1.7-win-64\\jre\\lib\\ext\\sunmscapi.jar;F:\\repos\\a1\\trunk\\vendors\\Java\\jdk-1.7-win-64\\jre\\lib\\ext\\zipfs.jar;F:\\dropbox\\Dropbox\\codereview\\out\\production\\common;C:\\Users\\p.yushkovskiy\\.gradle\\caches\\modules-2\\files-2.1\\com.intellij\\annotations\\12.0\\bbcf6448f6d40abe506e2c83b70a3e8bfd2b4539\\annotations-12.0.jar;C:\\Users\\p.yushkovskiy\\.gradle\\caches\\modules-2\\files-2.1\\javax.persistence\\persistence-api\\1.0.2\\8a7f96961047da52a53b7347b6a919c6fbc6d7fa\\persistence-api-1.0.2.jar;C:\\Users\\p.yushkovskiy\\.gradle\\caches\\modules-2\\files-2.1\\javax.ws.rs\\jsr311-api\\1.1.1\\59033da2a1afd56af1ac576750a8d0b1830d59e6\\jsr311-api-1.1.1.jar;C:\\Users\\p.yushkovskiy\\.gradle\\caches\\modules-2\\files-2.1\\org.aspectj\\aspectjrt\\1.7.4\\e49a5c0acee8fd66225dc1d031692d132323417f\\aspectjrt-1.7.4.jar;C:\\Users\\p.yushkovskiy\\.gradle\\caches\\modules-2\\files-2.1\\org.aspectj\\aspectjtools\\1.7.4\\23e0a79664fdb42bbf9116f639bc3c2c4012844f\\aspectjtools-1.7.4.jar;F:\\dropbox\\Dropbox\\codereview\\out\\production\\tools;C:\\Users\\p.yushkovskiy\\.gradle\\caches\\modules-2\\files-2.1\\org.aspectj\\aspectjweaver\\1.7.4\\d9d511e417710492f78bb0fb291a629d56bf4216\\aspectjweaver-1.7.4.jar;F:/dropbox/Dropbox/org.aspectj/out/artifacts/org_aspectj_ajdt_core_jar/org.aspectj.ajdt.core.jar",
        "-aspectpath", "F:\\repos\\a1\\trunk\\vendors\\Java\\jdk-1.7-win-64\\jre\\lib\\charsets.jar;F:\\repos\\a1\\trunk\\vendors\\Java\\jdk-1.7-win-64\\jre\\lib\\deploy.jar;F:\\repos\\a1\\trunk\\vendors\\Java\\jdk-1.7-win-64\\jre\\lib\\javaws.jar;F:\\repos\\a1\\trunk\\vendors\\Java\\jdk-1.7-win-64\\jre\\lib\\jce.jar;F:\\repos\\a1\\trunk\\vendors\\Java\\jdk-1.7-win-64\\jre\\lib\\jfr.jar;F:\\repos\\a1\\trunk\\vendors\\Java\\jdk-1.7-win-64\\jre\\lib\\jfxrt.jar;F:\\repos\\a1\\trunk\\vendors\\Java\\jdk-1.7-win-64\\jre\\lib\\jsse.jar;F:\\repos\\a1\\trunk\\vendors\\Java\\jdk-1.7-win-64\\jre\\lib\\management-agent.jar;F:\\repos\\a1\\trunk\\vendors\\Java\\jdk-1.7-win-64\\jre\\lib\\plugin.jar;F:\\repos\\a1\\trunk\\vendors\\Java\\jdk-1.7-win-64\\jre\\lib\\resources.jar;F:\\repos\\a1\\trunk\\vendors\\Java\\jdk-1.7-win-64\\jre\\lib\\rt.jar;F:\\repos\\a1\\trunk\\vendors\\Java\\jdk-1.7-win-64\\jre\\lib\\ext\\access-bridge-64.jar;F:\\repos\\a1\\trunk\\vendors\\Java\\jdk-1.7-win-64\\jre\\lib\\ext\\dnsns.jar;F:\\repos\\a1\\trunk\\vendors\\Java\\jdk-1.7-win-64\\jre\\lib\\ext\\jaccess.jar;F:\\repos\\a1\\trunk\\vendors\\Java\\jdk-1.7-win-64\\jre\\lib\\ext\\localedata.jar;F:\\repos\\a1\\trunk\\vendors\\Java\\jdk-1.7-win-64\\jre\\lib\\ext\\sunec.jar;F:\\repos\\a1\\trunk\\vendors\\Java\\jdk-1.7-win-64\\jre\\lib\\ext\\sunjce_provider.jar;F:\\repos\\a1\\trunk\\vendors\\Java\\jdk-1.7-win-64\\jre\\lib\\ext\\sunmscapi.jar;F:\\repos\\a1\\trunk\\vendors\\Java\\jdk-1.7-win-64\\jre\\lib\\ext\\zipfs.jar;F:\\dropbox\\Dropbox\\codereview\\out\\production\\common;C:\\Users\\p.yushkovskiy\\.gradle\\caches\\modules-2\\files-2.1\\com.intellij\\annotations\\12.0\\bbcf6448f6d40abe506e2c83b70a3e8bfd2b4539\\annotations-12.0.jar;C:\\Users\\p.yushkovskiy\\.gradle\\caches\\modules-2\\files-2.1\\javax.persistence\\persistence-api\\1.0.2\\8a7f96961047da52a53b7347b6a919c6fbc6d7fa\\persistence-api-1.0.2.jar;C:\\Users\\p.yushkovskiy\\.gradle\\caches\\modules-2\\files-2.1\\javax.ws.rs\\jsr311-api\\1.1.1\\59033da2a1afd56af1ac576750a8d0b1830d59e6\\jsr311-api-1.1.1.jar;C:\\Users\\p.yushkovskiy\\.gradle\\caches\\modules-2\\files-2.1\\org.aspectj\\aspectjrt\\1.7.4\\e49a5c0acee8fd66225dc1d031692d132323417f\\aspectjrt-1.7.4.jar;C:\\Users\\p.yushkovskiy\\.gradle\\caches\\modules-2\\files-2.1\\org.aspectj\\aspectjtools\\1.7.4\\23e0a79664fdb42bbf9116f639bc3c2c4012844f\\aspectjtools-1.7.4.jar;F:\\dropbox\\Dropbox\\codereview\\out\\production\\tools;C:\\Users\\p.yushkovskiy\\.gradle\\caches\\modules-2\\files-2.1\\org.aspectj\\aspectjweaver\\1.7.4\\d9d511e417710492f78bb0fb291a629d56bf4216\\aspectjweaver-1.7.4.jar",
        "-sourceroots", "F:\\dropbox\\Dropbox\\codereview\\common\\src\\main\\java"
    };
    final Ajc ajc = new Ajc();
    ajc.setBaseDir(baseDir);
    final CompilationResult result = ajc.compile(args);
    assertNoMessages(result);
  }

  public void testCompile() {
    final File baseDir = new File("../tests/base/test106");
    final String[] args = new String[]{"Driver.java", "pkg/Obj.java"};
    final CompilationResult result = ajc(baseDir, args);
    assertNoMessages(result);
    final RunResult rresult = run("Driver", new String[0], null);
    System.out.println(rresult.getStdOut());
  }

  public void testIncrementalCompile() throws Exception {
    final File baseDir = new File("../tests/incrementalju/initialTests/classAdded");
    final String[] args = new String[]{"-sourceroots", "src", "-d", ".", "-incremental"};
    CompilationResult result = ajc(baseDir, args);
    assertNoMessages(result);
    RunResult rr = run("main.Main", new String[0], null);
    // prepare for increment
    FileUtil.copyFile(new File(baseDir, "src.20/main/Main.java"),
        new File(ajc.getSandboxDirectory(), "src/main/Main.java"));
    assertFalse("main.Target does not exist", new File(ajc.getSandboxDirectory(), "main/Target.class").exists());
    result = ajc.doIncrementalCompile();
    assertNoMessages(result);
    assertTrue("main.Target created", new File(ajc.getSandboxDirectory(), "main/Target.class").exists());
    rr = run("main.Main", new String[0], null);
    System.out.println(rr.getStdOut());
  }

}
