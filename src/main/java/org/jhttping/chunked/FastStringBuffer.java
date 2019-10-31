package org.jhttping.chunked;

import java.util.ArrayList;
import java.util.List;

public class FastStringBuffer {
	
	private List<FastString> parts;
	private int length;
	private int offset;
	
	public FastStringBuffer() {
		parts = new ArrayList<FastString>();
		length = 0;
		offset = 0;
	}
	
	public void expand(FastString str) {
		parts.add(str);
		length+=str.length();
	}
	
	public void clear() {
		parts = new ArrayList<FastString>();
		length = 0;
		offset = 0;
	}
	
	public void contract(int length) {
		if (this.length < length) {
			throw new IndexOutOfBoundsException(this.length+":"+length);
		}
		int remaining = length;
		int firstPartLength = parts.get(0).length()-offset;
		while (remaining > 0 && firstPartLength <= remaining) {
			remaining-=firstPartLength;
			this.length-=firstPartLength;
			parts.remove(0);
			offset = 0;
			if (remaining > 0) {
				firstPartLength = parts.get(0).length();
			}
		}
		if (remaining > 0) {
			offset += remaining;
			this.length-=remaining;
		}
	}
	
	public int size() {
		return length;
	}
	
	public FastString toFastString() {
		FastString result = new FastString();
		for (int i = 0;i<parts.size(); i++) {
			FastString part = parts.get(i);
			if (i== 0 && offset > 0) {
				result = result.concat(part.substring(offset, part.length()));
			} else {
				result = result.concat(part);
			}
		}
		return result;
	}
	
	public byte charAt(int index) {
		if (length == 0) {
			return -1;
		} else {
			FastStringBufferCursor cursor = new FastStringBufferCursor();
			cursor.moveTo(index);
			return cursor.charAt();
		}
	}
	
	public int indexOf(FastString str, int fromIndex) {
		return indexOf(str.getBytes(), 0,str.length(), fromIndex);
	}
	
    private int indexOf(byte[] target, int targetOffset, int targetCount,
            int fromIndex) {
        if (fromIndex >= length) {
            return (targetCount == 0 ? length : -1);
        }
        if (fromIndex < 0) {
            fromIndex = 0;
        }
        if (targetCount == 0) {
            return fromIndex;
        }

        byte first = target[targetOffset];
        
        FastStringBufferCursor cursor = new FastStringBufferCursor();
        cursor.moveTo(fromIndex);
        boolean stop = false;
        while (!stop) {
        		/* Look for first character. */
        		while (cursor.charAt() != first && cursor.position < length-1) {
        			cursor.increment();
        		}
        		if (cursor.charAt() == first) {
	        		/* Found first character, now look at the rest of v2 */
        			if (targetCount == 1) {
                        /* Found whole string. */
                        return cursor.position;
        			} else {
        				int max =  (length - targetCount);
		                if (cursor.position <= max) {
		                    int end = cursor.position  + targetCount-1;
		                    FastStringBufferCursor cursor2 = cursor.createNewCursor();
		                    cursor2.increment();
		                    int k = targetOffset + 1;
		                    while (cursor2.position<end && cursor2.charAt() == target[k]) {
		                    	k++;
		                    	cursor2.increment();
		                    }
		
		                    if (cursor2.position == end && cursor2.charAt() == target[k]) {
		                        /* Found whole string. */
		                        return cursor.position;
		                    } else {
		                    	cursor.increment();
		                    }
		                } else {
		                	stop = true;
		                }
        			}    
        		} else {
        			stop = true;
        		}
        }
        return -1;
    }
    
    public FastString substring(int fromIndex, int toIndex) {
    	if (fromIndex < 0 || toIndex > length) {
    		throw new IndexOutOfBoundsException(fromIndex+":"+toIndex);
    	}
    	int targetLength = toIndex-fromIndex;
    	FastStringBufferCursor cursor = new FastStringBufferCursor();
    	cursor.moveTo(fromIndex);
    	FastString result = new FastString();
    	int partNumber = cursor.partNumber;
    	int index = cursor.index;
    	int remaining = targetLength; 
    	while (remaining>0) {
    		int partLength = parts.get(partNumber).length()-index;
    		int readLength = Math.min(partLength, remaining);
    		result = result.concat(parts.get(partNumber).substring(index, index+readLength));
    		remaining-=readLength;
    		if (remaining > 0) {
    			partNumber++;
    			index = 0;
    		}
    	}
    	return result;
    }
    
    public String toString() {
    	StringBuilder result = new StringBuilder();
    	result.append("offset = "+offset+";");
    	result.append("length = "+length+";");
    	for (FastString p: parts) {
    		result.append("part = "+p+";");
    	}
    	return result.toString();
    }
	
	
	
	private class FastStringBufferCursor {
		
		private int partNumber;
		private int index;
		private int position;
		
		private FastStringBufferCursor() {
			partNumber = 0;
			index = offset;
			position = 0;
		}
		
		private void moveTo(int index) {
			if (index<0 || index > length) {
				throw new IndexOutOfBoundsException(index+":"+length);
			}
			int newPartNumber = 0;
			int newIndex = offset;
			int runningIndex = index;
			int partLength = parts.get(newPartNumber).length()-newIndex;
			while (runningIndex > partLength-1) {
				runningIndex-=partLength;
				newPartNumber++;
				newIndex = 0;
				partLength = parts.get(newPartNumber).length();
			}
			newIndex+= runningIndex;
			
			partNumber = newPartNumber;
			this.index = newIndex;
			position = index;
		}
		
		private void increment() {
			FastString currentPart = parts.get(partNumber);
			if (index < currentPart.length()-1) {
				index++;
				position++;
			} else {
				if (partNumber == parts.size()-1) {
					throw new IndexOutOfBoundsException(partNumber+":"+index+":"+currentPart.length()+":"+parts.size());
				}
				partNumber++;
				index = 0;
				position++;
			}
		}
		
		
		private byte charAt() {
			return parts.get(partNumber).charAt(index);
		}
		
		
		private FastStringBufferCursor createNewCursor() {
			FastStringBufferCursor result = new FastStringBufferCursor();
			result.partNumber = partNumber;
			result.index = index;
			result.position = position;
			return result;
		}
		
	}

}
