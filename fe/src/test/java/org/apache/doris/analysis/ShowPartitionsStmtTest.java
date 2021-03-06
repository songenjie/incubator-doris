package org.apache.doris.analysis;

import org.apache.doris.catalog.Catalog;
import org.apache.doris.common.AnalysisException;
import org.apache.doris.common.UserException;
import org.apache.doris.system.SystemInfoService;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Arrays;


@RunWith(PowerMockRunner.class)
@PowerMockIgnore({ "org.apache.log4j.*", "javax.management.*" })
@PrepareForTest(Catalog.class)
public class ShowPartitionsStmtTest {
  private Analyzer analyzer;
  private Catalog catalog;
  private SystemInfoService systemInfo;

  @Rule
  public ExpectedException expectedEx = ExpectedException.none();

  @Before
  public void setUp() {
    catalog = AccessTestUtil.fetchAdminCatalog();
    systemInfo = new SystemInfoService();

    PowerMock.mockStatic(Catalog.class);
    EasyMock.expect(Catalog.getInstance()).andReturn(catalog).anyTimes();
    EasyMock.expect(Catalog.getCurrentSystemInfo()).andReturn(systemInfo).anyTimes();
    EasyMock.expect(Catalog.getCurrentCatalog()).andReturn(catalog).anyTimes();
    PowerMock.replay(Catalog.class);

    analyzer = EasyMock.createMock(Analyzer.class);
    EasyMock.expect(analyzer.getDefaultDb()).andReturn("testDb").anyTimes();
    EasyMock.expect(analyzer.getCatalog()).andReturn(catalog).anyTimes();
    EasyMock.expect(analyzer.getClusterName()).andReturn("testCluster").anyTimes();
    EasyMock.replay(analyzer);
  }

  @Test
  public void testNormal() throws UserException {
    ShowPartitionsStmt stmt = new ShowPartitionsStmt(new TableName("testDb", "testTable"), null, null, null);
    stmt.analyzeImpl(analyzer);
    Assert.assertEquals("SHOW PARTITIONS FROM `testCluster:testDb`.`testTable`", stmt.toString());
  }

  @Test
  public void testShowPartitionsStmtWithBinaryPredicate() throws UserException {
    SlotRef slotRef = new SlotRef(null, "LastConsistencyCheckTime");
    StringLiteral stringLiteral = new StringLiteral("2019-12-22 10:22:11");
    BinaryPredicate binaryPredicate = new BinaryPredicate(BinaryPredicate.Operator.GT, slotRef, stringLiteral);
    ShowPartitionsStmt stmt = new ShowPartitionsStmt(new TableName("testDb", "testTable"), binaryPredicate, null, null);
    stmt.analyzeImpl(analyzer);
    Assert.assertEquals("SHOW PARTITIONS FROM `testCluster:testDb`.`testTable` WHERE `LastConsistencyCheckTime` > '2019-12-22 10:22:11'", stmt.toString());
  }

  @Test
  public void testShowPartitionsStmtWithLikePredicate() throws UserException {
    SlotRef slotRef = new SlotRef(null, "PartitionName");
    StringLiteral stringLiteral = new StringLiteral("%p2019%");
    LikePredicate likePredicate = new LikePredicate(LikePredicate.Operator.LIKE, slotRef, stringLiteral);
    ShowPartitionsStmt stmt = new ShowPartitionsStmt(new TableName("testDb", "testTable"), likePredicate, null, null);
    stmt.analyzeImpl(analyzer);
    Assert.assertEquals("SHOW PARTITIONS FROM `testCluster:testDb`.`testTable` WHERE `PartitionName` LIKE '%p2019%'", stmt.toString());
  }

  @Test
  public void testShowParitionsStmtOrderByAndLimit() throws UserException {
    SlotRef slotRef = new SlotRef(null, "PartitionId");
    OrderByElement orderByElement = new OrderByElement(slotRef, true, false);
    LimitElement limitElement = new LimitElement(10);
    ShowPartitionsStmt stmt = new ShowPartitionsStmt(new TableName("testDb", "testTable"), null, Arrays.asList(orderByElement), limitElement);
    stmt.analyzeImpl(analyzer);
    Assert.assertEquals("SHOW PARTITIONS FROM `testCluster:testDb`.`testTable` ORDER BY `PartitionId` ASC LIMIT 10", stmt.toString());
  }

  @Test
  public void testUnsupportFilter() throws UserException {
    SlotRef slotRef = new SlotRef(null, "DataSize");
    StringLiteral stringLiteral = new StringLiteral("3.2 GB");
    BinaryPredicate binaryPredicate = new BinaryPredicate(BinaryPredicate.Operator.EQ, slotRef, stringLiteral);
    ShowPartitionsStmt stmt = new ShowPartitionsStmt(new TableName("testDb", "testTable"), binaryPredicate, null, null);
    expectedEx.expect(AnalysisException.class);
    expectedEx.expectMessage("Only the columns of PartitionId/PartitionName/" +
        "State/Buckets/ReplicationNum/LastConsistencyCheckTime are supported.");
    stmt.analyzeImpl(analyzer);
  }

}
