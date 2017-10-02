// author jeyakrishna ramamoorthy
// created on 02OCT2017

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MaxFeeTxHandler {

	UTXOPool utxoPool = null;
	
	public MaxFeeTxHandler(UTXOPool utxoPool) {
		this.utxoPool = utxoPool;
	}
	
	
	
	  public boolean isValidTx(Transaction tx) {
	    	ArrayList<Transaction.Input> inputs = tx.getInputs();
	    	double inputSum = 0;
	    	double outputSum = 0;
	    	
	    	for ( int index = 0; index < tx.numInputs(); index ++ ){
	    		Transaction.Input input = inputs.get(index);
	    		UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
	    		// if the specified input in the transaction is not an unspent txn
	        	// then return invalid
	    		if ( !utxoPool.contains(utxo)){
	    			return false;
	    		}
	    		
	    		// validate signature
	    		// does this paisa belong to you as you claim
	    		Transaction.Output output = utxoPool.getTxOutput(utxo);
	    		boolean isValidSign = Crypto.verifySignature
	    				(output.address, tx.getRawDataToSign(index), input.signature);
	    		if ( !isValidSign ) {
	    			return false; // invalid signature
	    		}
	    		
	    		// double spend validation
	    		for (int innerIndex = 0; innerIndex < tx.numInputs(); innerIndex++ ){
	    			Transaction.Input innerInput = inputs.get(innerIndex);
	    			if ( innerIndex != index ){
	    				UTXO innerUtxo = new UTXO(innerInput.prevTxHash, innerInput.outputIndex);
	    				if ( innerUtxo.equals(utxo)){
	    					return false;
	    				}
	    				
	    			}
	    		}
	    		
	    		// sum the input
	    		inputSum += output.value;
	    	
	    		
	    	}
	    	
	    	// validate for negative output
	    	ArrayList<Transaction.Output> outputs = tx.getOutputs();
	    	for (Transaction.Output output: outputs ){
	    		if ( output.value < 0 ){
	    			return false;
	    		}
	    		outputSum += output.value;
	    	}
	   
	    	
	    	// validate input >= output values
	    	if ( outputSum > inputSum ){
	    		return false;
	    	}


	    	return true;
	    }
	  
	  
	public Transaction[] handleTxs(Transaction[] possibleTxs) {
		Transaction[] validTxns = new Transaction[possibleTxs.length];
		Map<Transaction, Double> txnFeeMap = new HashMap<Transaction, Double>();
		int arrayIndex = 0;
		double maxFee = 0;
		double inputSum = 0;
		double outputSum = 0;
		double fees = 0;
		for (Transaction txn : possibleTxs) { // looping txns
			if ( !isValidTx(txn) )
				continue;
			
				inputSum = 0;
				outputSum = 0;
				// remove inputs from utxo pool as they are consumed
				ArrayList<Transaction.Input> inputs = txn.getInputs();
				for (int index = 0; index < txn.numInputs(); index++) {
					Transaction.Input input = inputs.get(index);
					UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
					Transaction.Output inputTxn = utxoPool.getTxOutput(utxo);
					inputSum += inputTxn.value;
				}
				for (int index = 0; index < txn.numOutputs(); index++) {
					if ( txn.getOutputs().get(index) != null ){
						outputSum += txn.getOutputs().get(index).value;
					}				
				}
				fees = inputSum - outputSum;
				if (fees > maxFee) {
					maxFee = fees;
				}
				txnFeeMap.put(txn, fees);

		} // looping txns		
		Double MAXFEE = maxFee;
		// filter txns with max fee
		for ( Transaction txn: txnFeeMap.keySet()){
			if ( txnFeeMap.get(txn).equals(MAXFEE)){
				validTxns[arrayIndex++] = txn;
			}
		}
		//System.out.println("max fee is: " + MAXFEE);	
		return validTxns;
	}
	

}
