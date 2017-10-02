// author jeyakrishna ramamoorthy
// created on 02OCT2017


import java.util.ArrayList;


public class TxHandler {
	
	UTXOPool utxPool = null;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
    	utxPool = new UTXOPool(utxoPool);
    	
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
    	ArrayList<Transaction.Input> inputs = tx.getInputs();
    	double inputSum = 0;
    	double outputSum = 0;
    	
    	for ( int index = 0; index < tx.numInputs(); index ++ ){
    		Transaction.Input input = inputs.get(index);
    		UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
    		// if the specified input in the transaction is not an unspent txn
        	// then return invalid
    		if ( !utxPool.contains(utxo)){
    			return false;
    		}
    		
    		// validate signature
    		// does this paisa belong to you as you claim
    		Transaction.Output output = utxPool.getTxOutput(utxo);
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

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
    	//ArrayList<Transaction> validTxns = new ArrayList<Transaction>();
    	Transaction[] validTxns = new Transaction[possibleTxs.length];
    	int arrayIndex = 0;
    	boolean validTxn = false;
    	for ( Transaction txn: possibleTxs){
    		validTxn = isValidTx(txn);
    		
    		if ( validTxn ) {
    			// remove inputs from utxo pool as they are consumed
    			ArrayList<Transaction.Input> inputs = txn.getInputs();
    			for ( int index = 0; index < txn.numInputs(); index ++ ){
    	    		Transaction.Input input = inputs.get(index);
    	    		UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
    	    		utxPool.removeUTXO(utxo); 	    			
    			}
    			// add outputs to utxo pool 
    			//ArrayList<Transaction.Output> outputs = txn.getOutputs();
    			for ( int index = 0; index < txn.numOutputs(); index ++ ){
    	    		UTXO utxo = new UTXO(txn.getHash(), index);
    	    		utxPool.addUTXO(utxo, txn.getOutputs().get(index));   	    		    			
    			}
    			validTxns[arrayIndex++] = txn;   			
    		}   		
    		
    	}
    	return validTxns;
    }

}
