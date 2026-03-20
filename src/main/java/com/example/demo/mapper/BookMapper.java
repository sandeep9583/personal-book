package com.example.demo.mapper;

import com.example.demo.db.Book;
import com.example.demo.google.GoogleBook;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;


@Mapper(componentModel = "spring")
public interface BookMapper {


    @Mapping(source = "id",                          target = "id")
    @Mapping(source = "volumeInfo.title",            target = "title")
    @Mapping(source = "volumeInfo.authors",          target = "author",    qualifiedByName = "firstAuthor")
    @Mapping(source = "volumeInfo.pageCount",        target = "pageCount")
    Book toBook(GoogleBook.Item item);


    @Named("firstAuthor")
    default String firstAuthor(List<String> authors) {
        if (authors == null || authors.isEmpty()) {
            return null;
        }
        return authors.get(0);
    }
}