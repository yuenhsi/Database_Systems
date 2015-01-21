/**
 * @author Yuen Hsi Chang
 * 
 * Slotted file page. This is a wrapper around a traditional Page that
 * adds the appropriate struture to it.
 */

import java.nio.*;
import java.util.Arrays;

public class SlottedPage
{
    public static class PageFullException extends RuntimeException {};
    public static class BadSlotIdException extends RuntimeException {};
    public static class BadPageIdException extends RuntimeException {};

    private static class SlotArrayOutOfBoundsException
        extends RuntimeException {};

    /**
     * Value to use for an invalid page id.
     */
    public static final int INVALID_PAGE = -1;
    public static final int SIZE_OF_INT = 4;

    private byte[] data;
    private IntBuffer intBuffer;
    private int intBufferLength;
    
    private int pageId;
    private int nextPageId;
    private int prevPageId;
	
	
    /**
     * Constructs a slotted page by wrapping around a page object already
     * provided.
     * @param page the page to be wrapped.
     */
    public SlottedPage(Page page)
    {
        data = page.data;
        intBuffer = (ByteBuffer.wrap(data)).asIntBuffer();
        intBufferLength = data.length / SIZE_OF_INT;
    }

    /**
     * Initializes values on the heap file page as necessary. This is
     * separated out from the constructor since it actually modifies
     * the page at hand, where as the constructor simply sets up the
     * mechanism.
     */
    public void init()
    {
    	int numEntries = 0;
    	intBuffer.put(0, numEntries);
        int endOfFreeSpaceIndex = Page.PAGESIZE - 1;
    	intBuffer.put(1, endOfFreeSpaceIndex);
    }


    /**
     * Sets the page id.
     * @param pageId the new page id.
     */
    public void setPageId(int pageId)
    {
    	this.pageId = pageId;
    }

    /**
     * Gets the page id.
     * @return the page id.
     */
    public int getPageId()
    {
        return this.pageId;
    }

    /**
     * Sets the next page id.
     * @param pageId the next page id.
     */
    public void setNextPageId(int pageId)
    {
    	this.nextPageId = pageId;
    }

    /**
     * Gets the next page id.
     * @return the next page id.
     */
    public int getNextPageId()
    {
        return nextPageId;
    }

    /**
     * Sets the previous page id.
     * @param pageId the previous page id.
     */
    public void setPrevPageId(int pageId)
    {
    	this.prevPageId = pageId;
    }

    /**
     * Gets the previous page id.
     * @return the previous page id.
     */
    public int getPrevPageId()
    {
        return prevPageId;
    }

    /**
     * Determines how much space, in bytes, is actually available on the page,
     * which depends on whether or not a new slot in the slot array is
     * needed. If a new spot in the slot array is needed, then the amount of
     * available space has to take this into consideration. In other words, the
     * space you need for the addition to the slot array shouldn't be included
     * as part of the available space, because from the user's perspective, it
     * isn't available for adding data.
     * @return the amount of available space in bytes
     */
    public int getAvailableSpace()
    {
        int numEntries = intBuffer.get(0);
        int emptySlots = 0;
        int nonEmptySlots = 0;
        int counter = 2;

        // Account for the number of empty slots in the header
        while (nonEmptySlots < numEntries) {
            if (intBuffer.get(counter) == 0) {
                emptySlots++;
                counter = counter + 2;
            }
            else {
                nonEmptySlots++;
                counter = counter + 2;
            }
        }
        // Calculate the int size of the header and multiply by 4 for the byte size
        int slotArraySize = emptySlots + nonEmptySlots;
        int headerSizeInBytes = (slotArraySize + 1) * 2 * 4;
        int freeSpaceInBytes = intBuffer.get(1) - headerSizeInBytes -7;
        if (freeSpaceInBytes < 0) {
            freeSpaceInBytes = 0;
        }
        return freeSpaceInBytes;
    }
        

    /**
     * Dumps out to the screen the # of entries on the page, the location where
     * the free space starts, the slot array in a readable dashion, and the
     * actual contents of each record. (This method merely exists for debugging
     * and testing purposes.)
    */ 
    public void dumpPage()
    {
    	int numEntries = intBuffer.get(0);
        int emptySlots = 0;
        int nonEmptySlots = 0;
        int counter = 2;

        // calculate the size of the header, accounting for empty slots
        while (nonEmptySlots < numEntries) {
            if (intBuffer.get(counter) == 0) {
                emptySlots++;
                counter = counter + 2;
            }
            else {
                nonEmptySlots++;
                counter = counter + 2;
            }
        }
        int slotArraySize = emptySlots + nonEmptySlots;
        int headerSize = (slotArraySize + 1) * 2;

        System.out.println("\n--------PageDump--------");
    	System.out.println("Number of entries: " + intBuffer.get(0));
    	System.out.println("Free space starts at bit " + (headerSize * 4 + 1));
    	if (empty()) {
            // indicate that slot array is empty
    		System.out.println("Slot array is empty!");
    	}
		else {
			// print slot array
			System.out.println("Slot array: ");
			for (int i = 1; i < slotArraySize + 1; i++) {
                System.out.println("    slot: " + i);
                if (intBuffer.get(i * 2) == 0) {
                    System.out.println("This slot is empty!");
                }
                else {
    				System.out.println("	index: " + intBuffer.get(i* 2));
    				System.out.println("	size: " + intBuffer.get((i* 2) + 1));
                    byte[] contents = getRecord(new RID(pageId, i));
                    System.out.println("        contents: " + Arrays.toString(contents));
                }
			}
            System.out.println();
		}    	    	
    }

    /**
     * Inserts a new record onto the page.
     * @param record the record to be inserted. A copy of the data is
     * placed on the page.
     * @return the RID of the new record 
     * @throws PageFullException if there is not enough room for the
     * record on the page.
    */
    public RID insertRecord(byte[] record)
    {
        int recordLength = record.length;
        if (getAvailableSpace() >= recordLength) {
            for (int i = 2; i < 256; i++) {
                if (intBuffer.get(i) == 0) {
                    intBuffer.put((i), intBuffer.get(1) - recordLength);
                    intBuffer.put((i + 1), recordLength);

                    // add one to the number of entries
                    intBuffer.put(0, intBuffer.get(0) + 1);
                    // move the end of free space back by the length of the record
                    intBuffer.put(1, intBuffer.get(1) - recordLength);

                    for (int j = intBuffer.get(1), k = 0; k < recordLength; j++, k++) {
                        // data refers to free space, whereas record refers to byte data
                        data[j] = record[k];
                    }
                    RID result = new RID(this.pageId, i / 2);
                    return result;
                }
            }
            throw new PageFullException();
        }
        else {
            throw new PageFullException();  
        }
    }

    /**
     * Deletes the record with the given RID from the page, compacting
     * the hole created. Compacting the hole, in turn, requires that
     * all the offsets (in the slot array) of all records after the
     * hole be adjusted by the size of the hole, because you are
     * moving these records to "fill" the hole. You should leave a
     * "hole" in the slot array for the slot which pointed to the
     * deleted record, if necessary, to make sure that the rids of the
     * remaining records do not change. The slot array should be
     * compacted only if the record corresponding to the last slot is
     * being deleted.
     * @param rid the RID to be deleted.
     * @return true if successful, false if the rid is actually not
     * found on the page.
    */
    public boolean deleteRecord(RID rid)
    {
        if (rid.pageId != pageId) {
            return false;
        }
        if (intBuffer.get(rid.slotNum * 2) == 0){
            return false;
        }
        int loc = intBuffer.get(rid.slotNum * 2);
        int len = intBuffer.get((rid.slotNum * 2) + 1);
        // decrease the number of entries and clear the record's corresponding slot array
        int entryNum = intBuffer.get(0);
        intBuffer.put(0, entryNum - 1);
        intBuffer.put(rid.slotNum * 2, 0);
        intBuffer.put((rid.slotNum * 2) + 1, 0);

        // clear the record
        for (int i = loc; i < loc + len; i++) {
            data[i] = 0;
        }

        int numEntries = intBuffer.get(0);
        int j = 2;
        int i = 0;
        // check for records whose index preceeds the record that was removed
        while (i < numEntries) {
            int curLoc = intBuffer.get(j);
            if (curLoc != 0) {
                i++;
                if (curLoc < loc) {
                    // move these records's location, then update the slot array's pointer
                    int curLen = intBuffer.get(j + 1);
                    byte[] temp = new byte[curLen];
                    for(int a = 0, b = curLoc; a < curLen; a++, b++) {
                        temp[a] = data[b];
                        data[b] = 0;
                    }
                    int newLoc = curLoc + len;
                    intBuffer.put(j, newLoc);
                    for (int c = intBuffer.get(j), d = 0; d < curLen; c++, d++) {
                        data[c] = temp[d];
                    }   
                }
            }
            j = j + 2;
        }
        // move the end of free space back by the length of the record
        intBuffer.put(1, intBuffer.get(1) + len);
        return true;
    }

    /**
     * Returns RID of first record on page. Remember that some slots may be
     * empty, so you should skip over these.
     * @return the RID of the first record on the page. Returns null
     * if the page is empty.
     */
    public RID firstRecord()
    {
        for (int i = 2; i < 256; i++) {
            if (intBuffer.get(i) != 0) {
                return new RID(pageId, i / 2);
            }
            else {
                i++;
            }
        }
        return null;
    }

    /**
     * Returns RID of next record on the page, where "next on the page" means
     * "next in the slot array after the rid passed in." Remember that some
     * slots may be empty, so you should skip over these.
     * @param curRid an RID
     * @return the RID immediately following curRid. Returns null if
     * curRID is the last record on the page.
     * @throws BadPageIdException if the page id within curRid is
     * invalid
     * @throws BadSlotIdException if the slot id within curRid is invalid
    */
    public RID nextRecord(RID curRid)
    {
        if (curRid.pageId != pageId) {
            throw new BadPageIdException();            
        }
        if (intBuffer.get(curRid.slotNum * 2) == 0){
            throw new BadSlotIdException();
        }
        int numEntries = intBuffer.get(0);
        int counter = 2, nonEmptySlots = 0;
        while (nonEmptySlots < numEntries) {
            if (intBuffer.get(counter) != 0) {
                nonEmptySlots++;              
            }
            counter = counter + 2;
        }
        if ((counter - 2) / 2 == curRid.slotNum) {
            return null;
        }
        else {
            int nextSlot = curRid.slotNum * 2 + 2;
            while (intBuffer.get(nextSlot) == 0) {
                nextSlot = nextSlot + 2;
            }
            return new RID(curRid.pageId, nextSlot / 2);
        }
    }

    /**
     * Returns the record associated with an RID.
     * @param rid the rid of interest
     * @return a byte array containing a copy of the record. The array
     * has precisely the length of the record (there is no padded space).
     * @throws BadPageIdException if the page id within curRid is
     * invalid
     * @throws BadSlotIdException if the slot id within curRid is invalid
    */
    public byte[] getRecord(RID rid)
    {
    	//checks if we are on the right page
        if (rid.pageId != pageId){
        	throw new BadPageIdException();
        }        
        //checks if there is something in the slot
        if (intBuffer.get(rid.slotNum * 2) == 0){
        	throw new BadSlotIdException();
        }
        else {
        	//gets location and size of the entry
        	int loc = intBuffer.get(rid.slotNum * 2);
        	int len = intBuffer.get(rid.slotNum * 2 + 1);
        	
        	//copies entry into a new array to be returned
        	byte[] result = new byte[len];
        	for(int i = 0, j = loc; i < len; i++, j++) {
        		result[i] = data[j];
        	}
        	return result;
        }
    }

    /**
     * Whether or not the page is empty.
     * @return true if the page is empty, false otherwise.
     */
    public boolean empty()
    {
    	// if there are no entries on the page, the page is empty. 
    	if (intBuffer.get(0) == 0) {
    		return true;
    	}
    	else {
    		return false;
    	}
    }
    
    public void printAll()
    {
    	for (int i = 0; i <= 1023; i++) {
    		System.out.print(data[i]);
    		if((i+1)%4 == 0){
    			System.out.print(" ");
    			if((i+1)%16==0){
    				System.out.println();
    			}
    		}
    	}
    }
}
