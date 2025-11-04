package com.thereadingroom.model.entity;

/**
 * Represents a book entity with attributes such as ID, title, author, available
 * copies, price, and sold copies.
 * This class provides getters and setters for manipulating book data.
 */
public class Book {
    private int id; // Unique identifier for the book
    private String title; // The title of the book
    private String author; // The author of the book
    private int physicalCopies; // Number of physical copies available
    private double price; // Price of the book
    private int soldCopies; // Number of sold copies of the book

    /**
     * Constructor to initialize a book instance with its attributes.
     *
     * @param id             Unique identifier for the book
     * @param title          Title of the book
     * @param author         Author of the book
     * @param physicalCopies Number of physical copies available
     * @param price          Price of the book
     * @param soldCopies     Number of copies sold
     */
    public Book(int id, String title, String author, int physicalCopies, double price, int soldCopies) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.physicalCopies = physicalCopies;
        this.price = price;
        this.soldCopies = soldCopies;
    }

    // Getter for the book's unique identifier
    public int getBookId() {
        return id;
    }

    // Setter for the book's unique identifier
    public void setBookId(int id) {
        this.id = id;
    }

    // Getter for the book's title
    public String getTitle() {
        return title;
    }

    // Setter for the book's title
    public void setTitle(String title) {
        this.title = title;
    }

    // Getter for the book's author
    public String getAuthor() {
        return author;
    }

    // Setter for the book's author
    public void setAuthor(String author) {
        this.author = author;
    }

    // Getter for the number of physical copies available
    public int getPhysicalCopies() {
        return physicalCopies;
    }

    // Setter for the number of physical copies available
    public void setPhysicalCopies(int physicalCopies) {
        this.physicalCopies = physicalCopies;
    }

    // Getter for the book's price
    public double getPrice() {
        return price;
    }

    // Setter for the book's price
    public void setPrice(double price) {
        this.price = price;
    }

    // Getter for the number of sold copies
    public int getSoldCopies() {
        return soldCopies;
    }

    // Setter for the number of sold copies
    public void setSoldCopies(int soldCopies) {
        this.soldCopies = soldCopies;
    }

    /**
     * Compares this Book with another object for equality.
     * Two Book objects are considered equal if they have the same ID.
     *
     * @param obj The object to compare with.
     * @return true if the objects are equal (same ID), false otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Book book = (Book) obj;
        return id == book.id;
    }

    /**
     * Returns a hash code value for this Book object.
     * The hash code is based on the book's unique ID.
     *
     * @return A hash code value for this Book.
     */
    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}
