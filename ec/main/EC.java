package ec.main;

import java.io.IOException;
import java.io.Serializable;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import ec.exceptions.InitializationException;

public interface EC extends Serializable {
	
	
	public void start(Properties p) throws IOException, InterruptedException, 
	ExecutionException, InitializationException;

}