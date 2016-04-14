package de.idadachverband.process;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Created by boehm on 19.02.15.
 */
@RequiredArgsConstructor
public enum ProcessStep
{ 
    upload("upload"), workingFormat("workingformat"), solrFormat("solr");
    
    @Getter
    private final String name;
}
