/**
 * @author Yuen Hsi Chang
 */
import java.io.*;
import java.util.*;

public class SPTester
{
    public static interface Testable
    {
        void test() throws Exception;
    }
    
    public static class TestFailedException extends RuntimeException
    {
        public TestFailedException(String explanation)
        {
            super(explanation);
        }
    }

    public static class Test1 implements Testable
    {
        public void test() throws Exception
        {
            SlottedPage sp = new SlottedPage(new Page());
            sp.init();
            
            System.out.println("--- Test 1: Page Initialization Checks ---");
            sp.setPageId(7);
            sp.setNextPageId(8);
            sp.setPrevPageId(SlottedPage.INVALID_PAGE);
            
            System.out.println
                ("Current Page No.: " + sp.getPageId() + ", " +
                 "Next Page Id: " + sp.getNextPageId() + ", " +
                 "Prev Page Id: " + sp.getPrevPageId() + ", " +
                 "Available Space: " + sp.getAvailableSpace());
        
            if (!sp.empty())
                throw new TestFailedException("Page should be empty.");

            System.out.println("Page Empty as expected.");
            sp.dumpPage();
            System.out.println();
        }
    }


    public static class Test2 implements Testable
    {
        public void test() throws Exception
        {
            int buffSize = 20;
            int limit = 20;
            byte[] tmpBuf = new byte[buffSize];

            SlottedPage sp = new SlottedPage(new Page());
            sp.init();
            sp.setPageId(7);
            sp.setNextPageId(8);
            sp.setPrevPageId(SlottedPage.INVALID_PAGE);

            System.out.println("--- Test 2: Insert and traversal of " +
                               "records ---");
            for (int i=0; i < limit; i++)
            {
                RID rid = sp.insertRecord(tmpBuf);
                System.out.println("Inserted record, RID " + rid.pageId +
                                   ", " + rid.slotNum);
                rid = sp.nextRecord(rid);
            }

            if (sp.empty())
                throw new TestFailedException("The page cannot be empty");

            RID rid = sp.firstRecord();
            while (rid != null)
            {
                tmpBuf = sp.getRecord(rid); 
                System.out.println("Retrieved record, RID " + rid.pageId +
                                   ", " + rid.slotNum);
                rid = sp.nextRecord(rid);
            }            
            sp.dumpPage();
        }
    }

    public static class Test3 implements Testable
    {
        public void test() throws Exception
        {
            int a = 500;
            int b = 492;

            byte[] tmpBuf = new byte[a];
            byte[] tmpBuf2 = new byte[b];

            for (int i = 0; i < 500; i++) {
                tmpBuf[i] = (byte)(i + 1);
            }
            for (int i = 0; i < 492; i++) {
                tmpBuf2[i] = (byte)(i + 1);
            }

            SlottedPage sp = new SlottedPage(new Page());
            sp.init();
            sp.setPageId(7);
            sp.setNextPageId(8);
            sp.setPrevPageId(SlottedPage.INVALID_PAGE);

            System.out.println("--- Test 3: Deletion of records ---");

            RID rid1 = sp.insertRecord(tmpBuf);
            System.out.println("Inserted record, RID " + rid1.pageId + ", " + rid1.slotNum);
            RID rid2 = sp.insertRecord(tmpBuf2);
            System.out.println("Inserted record, RID " + rid2.pageId + ", " + rid2.slotNum);

            sp.dumpPage();

            sp.deleteRecord(rid2);
            System.out.println("Deleted record, RID " + rid2.pageId + ", " + rid2.slotNum);
            sp.dumpPage();

            rid2 = sp.insertRecord(tmpBuf2);
            System.out.println("Inserted record, RID " + rid2.pageId + ", " + rid2.slotNum);

            sp.dumpPage();
        }
    }

    public static class Test4 implements Testable
    {
        public void test() throws Exception
        {
            int a = 10;
            int b = 15;
            int c = 20;

            byte[] tmpBuf = new byte[a];
            byte[] tmpBuf2 = new byte[b];
            byte[] tmpBuf3 = new byte[c];

            for (int i = 0; i < 10; i++) {
                tmpBuf[i] = (byte)(i);
            }
            for (int i = 0; i < 15; i++) {
                tmpBuf2[i] = (byte)(i + 10);
            }
            for (int i = 0; i < 20; i++) {
                tmpBuf3[i] = (byte)(i + 20);
            }

            SlottedPage sp = new SlottedPage(new Page());
            sp.init();
            sp.setPageId(7);
            sp.setNextPageId(8);
            sp.setPrevPageId(SlottedPage.INVALID_PAGE);

            System.out.println("--- Test 3: Deletion of records ---");

            RID rid1 = sp.insertRecord(tmpBuf);
            System.out.println("Inserted record, RID " + rid1.pageId + ", " + rid1.slotNum);
            RID rid2 = sp.insertRecord(tmpBuf2);
            System.out.println("Inserted record, RID " + rid2.pageId + ", " + rid2.slotNum);
            RID rid3 = sp.insertRecord(tmpBuf3);
            System.out.println("Inserted record, RID " + rid3.pageId + ", " + rid3.slotNum);

            sp.dumpPage();

            sp.deleteRecord(rid2);
            System.out.println("Deleted record, RID " + rid2.pageId + ", " + rid2.slotNum);

            sp.dumpPage();
            sp.printAll();
        }
    }

    public static boolean runTest(Testable testObj)
    {
        boolean success = true;
        try
        {
            testObj.test();
        }
        catch (Exception e)
        {
            success = false;
            e.printStackTrace();
        }
        return success;
    }


    public static void main(String[] args)
    {
        System.out.println("Running page tests.");

		SlottedPage sp = new SlottedPage(new Page());
        sp.init();
        runTest(new Test1());
        runTest(new Test2());   
        runTest(new Test3());   
        runTest(new Test4());   
    }
}
