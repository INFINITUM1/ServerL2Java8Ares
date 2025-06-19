package ru.agecold.util.reference;

public interface HardReference<T>
{
	public T get();
	
	public void clear();
}
