package main.collections;

public class StreamPair<A, B> {
    private A first;
    private B second;
    public StreamPair(final A first, final B second)
    {
        this.first = first;
        this.second = second;
    }
    public A getFirst()
    {
        return first;
    }
    public B getSecond()
    {
        return second;
    }
}
