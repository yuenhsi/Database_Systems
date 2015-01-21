import java.io.*;
import java.util.*;

/**
 * Buffer manager. Manages a memory-based buffer pool of pages.
 * @author Yuen Hsi Chang, with considerable material reused from the
 * UW-Madison Minibase project
 */
public class BufferManager
{
    public static class PageNotPinnedException
        extends RuntimeException {};
    public static class PagePinnedException extends RuntimeException {};

    /**
     * Value to use for an invalid page id.
     */
    public static final int INVALID_PAGE = -1;

    private static class FrameDescriptor
    {
        private int pageNum;
        private int pinCount;
        private String fileName;
        private boolean dirty;
        private boolean reference;
        
        public FrameDescriptor()
        {
            pageNum = INVALID_PAGE;
            pinCount = 0;
            fileName = null;
            dirty = false;
            reference = true;
        }

    }

    // Here are some private variables to get you started. You'll
    // probably need more.
    private Page[] bufferPool;
    private FrameDescriptor[] frameTable;
    int clockPointer;
    private HashMap<Integer, Integer> map;

    /**
     * Creates a buffer manager with the specified size.
     * @param poolSize the number of pages that the buffer pool can hold.
     */
    public BufferManager(int poolSize)
    {
    	bufferPool = new Page[poolSize];
    	frameTable = new FrameDescriptor[poolSize];
    	clockPointer = 0;
    	map = new HashMap<Integer, Integer>();
    }

    /**
     * Returns the pool size.
     * @return the pool size.
     */
    public int poolSize()
    {
    	return bufferPool.length;
    }

    /**
     * Checks if this page is in buffer pool. If it is, returns a
     * pointer to it. Otherwise, it finds an available frame for this
     * page, reads the page, and pins it. Writes out the old page, if
     * it is dirty, before reading.
     * @param pinPageId the page id for the page to be pinned
     * @param fileName the name of the database that contains the page
     * to be pinned
     * @param emptyPage determines if the page is known to be
     * empty. If true, then the page is not actually read from disk
     * since it is assumed to be empty.
     * @return a reference to the page in the buffer pool. If the buffer
     * pool is full, null is returned.
     * @throws IOException passed through from underlying file system.
     */
    public Page pinPage(int pinPageId, String fileName, boolean emptyPage)
        throws IOException
    {    	
    	// If page exists in bufferpool, return a pointer and pin it
    	if (map.get(pinPageId) != null){
    		int i = map.get(pinPageId);
    		frameTable[i].pinCount++;
    		return bufferPool[i];
		}		
		// If the page does not exists in the bufferpool
		int clockIterCount = clockPointer + (poolSize() * 2);
		
		for (int i = clockPointer; i < clockIterCount; i++) {
			int index = i % poolSize();
			// if the bufferpool is not full
			if (frameTable[index] == null) {
				// increment clock pointer
				clockPointer = (clockPointer + 1) % poolSize();

				// create the frame descriptor to be added
				FrameDescriptor temp = new FrameDescriptor();
				temp.pageNum = pinPageId;
				temp.pinCount = 1;
				temp.fileName = fileName;
				temp.reference = true;
				frameTable[index] = temp;
				
				// if the page to be added is empty
				if (emptyPage) {
					bufferPool[index] = new Page();
				}
				// if the page to be added is not empty
				else {							
					bufferPool[index] = new Page();
					// copy data from disk to the buffer pool
					new DBFile(fileName).readPage(pinPageId, bufferPool[index]);
				}
				map.put(pinPageId, index);
				return bufferPool[index];
			}
			// if we get here, the bufferpool is full as at least one item is not null
    		if (frameTable[index].pinCount == 0) {
    			// if the current page is not pinned and the reference bit is set
    			if (frameTable[index].reference == true){
    				// flip the reference bit
    				frameTable[index].reference = false;
    				clockPointer = (clockPointer + 1) % poolSize();
    			}
    			// if the current page is not pinned and the reference bit is not set, replace page
    			else {
					// if the page is dirty, flush it first
    				if (frameTable[index].dirty){
    					flushPage(frameTable[index].pageNum, frameTable[index].fileName);
    					map.remove(frameTable[index].pageNum);
    				}
    				// update the frame descriptor in the frame table
    				FrameDescriptor temp = frameTable[index];
    				temp.pageNum = pinPageId;
					temp.pinCount = 1;
					temp.fileName = fileName;
					temp.reference = true;
					frameTable[index] = temp;
					// if the page to be added is empty
					if (emptyPage) {
						bufferPool[index] = new Page();
					}
					// if the page to be added is not empty
					else {
						// read the page contents in the database and copies them to the buffer pool
						new DBFile(fileName).readPage(pinPageId, bufferPool[index]);
					}
					clockPointer = (i + 1) % poolSize();
					map.put(pinPageId, index);
					return bufferPool[index];
    			}
    		}
    	}		
    	// if everything is pinned
		return null;
    }

    /**
     * If the pin count for this page is greater than 0, it is
     * decremented. If the pin count becomes zero, it is appropriately
     * included in a group of replacement candidates.
     * @param unpinPageId the page id for the page to be unpinned
     * @param fileName the name of the database that contains the page
     * to be unpinned
     * @param dirty if false, then the page does not actually need to
     * be written back to disk.
     * @throws PageNotPinnedException if the page is not pinned, or if
     * the page id is invalid in some other way.
     */
    public void unpinPage(int unpinPageId, String fileName, boolean dirty)
    // The list of pinned and unpinned pages is recorded in our hash map, such 
    // that we don't need to read from the underlying file system. Therefore, 
    // we don't throw an IOException here. 
    {
    	if (map.get(unpinPageId) != null){
    		int i = map.get(unpinPageId);
    		if (frameTable[i].pinCount > 0) {
    			frameTable[i].pinCount--;
    			frameTable[i].dirty = dirty;
    			return;
    		}
    		else {
    			throw new PageNotPinnedException();
    		}   		
		}
    }


    /**
     * Requests a run of pages from the underlying database, then
     * finds a frame in the buffer pool for the first page and pins
     * it. If the buffer pool is full, no new pages are allocated from
     * the database.
     * @param numPages the number of pages in the run to be allocated.
     * @param fileName the name of the database from where pages are
     * to be allocated.
     * @return an Integer containing the first page id of the run, and
     * a references to the Page which has been pinned in the buffer
     * pool. Returns null if there is not enough space in the buffer
     * pool for the first page.
     * @throws DBFile.FileFullException if there are not enough free pages.
     * @throws IOException passed through from underlying file system.
     */
    public Pair<Integer,Page> newPage(int numPages, String fileName)
        throws IOException
    {
    	// find a page in page[] to write to. Throws IOException. 
    	DBFile db = new DBFile(fileName);
    	
    	// throws FileFullException
    	int index = db.allocatePages(numPages);
    	Page toReturn = pinPage(index, fileName, true);
    	
    	return new Pair<Integer, Page>(index, toReturn);
    }

    /**
     * Deallocates a page from the underlying database. Verifies that
     * page is not pinned.
     * @param pageId the page id to be deallocated.
     * @param fileName the name of the database from where the page is
     * to be deallocated.
     * @throws PagePinnedException if the page is pinned
     * @throws IOException passed through from underlying file system.
     */
    public void freePage(int pageId, String fileName) throws IOException
	{
    	if (map.get(pageId) == null) {
    		return;
    	}
    	int index = map.get(pageId);
    	
    	if (frameTable[index].pinCount == 0){
    		// throws IOException
    		new DBFile(fileName).deallocatePages(frameTable[index].pageNum, 1);
    	}
    	else {
    		throw new PagePinnedException();
    	}
    }

    /**
     * Flushes page from the buffer pool to the underlying database if
     * it is dirty. If page is not dirty, it is not flushed,
     * especially since an undirty page may hang around even after the
     * underlying database has been erased. If the page is not in the
     * buffer pool, do nothing, since the page is effectively flushed
     * already.
     * @param pageId the page id to be flushed.
     * @param fileName the name of the database where the page should
     * be flushed.
     * @throws IOException passed through from underlying file system.
     */
    public void flushPage(int pageId, String fileName) throws IOException
    {
    	Page toWrite = null;
    	if (map.get(pageId) != null){
    		int i = map.get(pageId);
    		toWrite = bufferPool[i];
    		new DBFile(fileName).writePage(pageId, toWrite); //throws IOException
    	}
    }

    /**
     * Flushes all dirty pages from the buffer pool to the underlying
     * databases. If page is not dirty, it is not flushed, especially
     * since an undirty page may hang around even after the underlying
     * database has been erased.
     * @throws IOException passed through from underlying file system.
     */
    public void flushAllPages() throws IOException
    {
    	for (FrameDescriptor f: frameTable){
    		flushPage(f.pageNum, f.fileName); //flushPage throws IOException
    	}
    }
        
    /**
     * Returns buffer pool location for a particular pageId. This
     * method is just used for testing purposes: it probably doesn't
     * serve a real purpose in an actual database system.
     * @param pageId the page id to be looked up.
     * @param fileName the file name to be looked up.
     * @return the frame location for the page of interested. Returns
     * -1 if the page is not in the pool.
    */
    public int findFrame(int pageId, String fileName)
    {
    	return map.get(pageId);
    }
}
