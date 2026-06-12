package central;

import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;

public class Central {
    // BOURRYTO - TODO: mutex
    public Queue<String> incomingMessages = new LinkedList<String>();
    public Queue<String> outgoingMessages = new LinkedList<String>();

    public void addIncomingMessage(String message) {
        incomingMessages.add(message);
    }

    public void addOutgoingMessage(String message) {
        outgoingMessages.add(message);
        System.out.println("added outgoing " + message);
    }

    public String getNextOutgoingMessage() throws NoSuchElementException {
        return outgoingMessages.remove();
    }
}
