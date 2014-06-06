package musaic.musicEntities;

import java.io.IOException;
import java.io.Serializable;

import musaic.webAPIs.EchoNestQuery.EchoNestException;

public interface MusicEntity extends Serializable
{
	public void fillInEchoNestInfo() throws IOException, EchoNestException;
	public String toString();
	public boolean equals(Object songObj);
}