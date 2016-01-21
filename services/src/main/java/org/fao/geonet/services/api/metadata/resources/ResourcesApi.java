/*
 * =============================================================================
 * ===	Copyright (C) 2001-2016 Food and Agriculture Organization of the
 * ===	United Nations (FAO-UN), United Nations World Food Programme (WFP)
 * ===	and United Nations Environment Programme (UNEP)
 * ===
 * ===	This program is free software; you can redistribute it and/or modify
 * ===	it under the terms of the GNU General Public License as published by
 * ===	the Free Software Foundation; either version 2 of the License, or (at
 * ===	your option) any later version.
 * ===
 * ===	This program is distributed in the hope that it will be useful, but
 * ===	WITHOUT ANY WARRANTY; without even the implied warranty of
 * ===	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * ===	General Public License for more details.
 * ===
 * ===	You should have received a copy of the GNU General Public License
 * ===	along with this program; if not, write to the Free Software
 * ===	Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301, USA
 * ===
 * ===	Contact: Jeroen Ticheler - FAO - Viale delle Terme di Caracalla 2,
 * ===	Rome - Italy. email: geonetwork@osgeo.org
 * ==============================================================================
 */

package org.fao.geonet.services.api.metadata.resources;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.fao.geonet.ApplicationContextHolder;
import org.fao.geonet.domain.MetadataResource;
import org.fao.geonet.domain.MetadataResourceVisibility;
import org.fao.geonet.domain.MetadataResourceVisibilityConverter;
import org.fao.geonet.services.api.API;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.UnsatisfiedServletRequestParameterException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import javax.annotation.PostConstruct;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Metadata resource related operations.
 *
 * Load the store with id 'resourceStore'.
 */
@EnableWebMvc
@Service
@RequestMapping(value = {
        "/api/metadata/{metadataUuid}/resources",
        "/api/" + API.VERSION_0_1 +
                "/metadata/{metadataUuid}/resources"
})
@Api(value = "metadata/resources",
     tags= "metadata/resources",
     description = "Metadata resource related operations")
public class ResourcesApi {
    public ResourcesApi() {
    }
    public ResourcesApi(Store store) {
        this.store = store;
    }

    private Store store;

    public Store getStore() {
        return store;
    }

    public void setStore(Store store) {
        this.store = store;
    }

    private final ApplicationContext appContext = ApplicationContextHolder.get();

    @SuppressWarnings("unchecked")
    @PostConstruct
    public void init() {
        if (appContext != null) {
            this.store = appContext.getBean("resourceStore", Store.class);
        }
    }



    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(MetadataResourceVisibility.class, new MetadataResourceVisibilityConverter());
        binder.registerCustomEditor(Sort.class, new SortConverter());
    }


    public List<MetadataResource> getResources() {
        return null;
    }



    @ApiOperation(value = "List all metadata resources",
                  nickname = "getAllMetadataResources")
    @RequestMapping(method = RequestMethod.GET,
                    produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<MetadataResource> getAllResources(
                                       @ApiParam(value = "The metadata UUID",
                                                 required = true,
                                                 example = "43d7c186-2187-4bcd-8843-41e575a5ef56")
                                       @PathVariable
                                       String metadataUuid,
                                       @ApiParam(value = "Sort by",
                                                 example = "type")
                                       @RequestParam(required = false,
                                                     defaultValue = "name")
                                       Sort sort,
                                       @RequestParam(required = false,
                                                     defaultValue = FilesystemStore.DEFAULT_FILTER)
                                       String filter) throws Exception {
        return store.getResources(metadataUuid, sort, filter);
    }



    @ApiOperation(value = "Delete all uploaded metadata resources",
                  nickname = "deleteAllMetadataResources")
    @RequestMapping(method = RequestMethod.DELETE)
    @PreAuthorize("hasRole('Editor')")
    @ResponseBody
    public boolean delResources(@ApiParam(value = "The metadata UUID",
                                          required = true,
                                          example = "43d7c186-2187-4bcd-8843-41e575a5ef56")
                                @PathVariable
                                String metadataUuid) throws Exception {
        store.delResource(metadataUuid);
        return true;
    }



    @ApiOperation(value = "Create a new resource for a given metadata",
                  nickname = "putResourceFromFile")
    @RequestMapping(method = RequestMethod.POST)
    @PreAuthorize("hasRole('Editor')")
    @ResponseBody
    public List<MetadataResource> putResource(
                                @ApiParam(value = "The metadata UUID",
                                          required = true,
                                          example = "43d7c186-2187-4bcd-8843-41e575a5ef56")
                                @PathVariable
                                String metadataUuid,
                                @ApiParam(value = "The sharing policy",
                                          example = "public")
                                @RequestParam(required = false,
                                              defaultValue = "public")
                                MetadataResourceVisibility visibility,
                                @ApiParam(value = "The file to upload")
                                @RequestParam("file")
                                List<MultipartFile> files)
            throws Exception {
        List<MetadataResource> resources = new ArrayList<>();
        for(MultipartFile file : files) {
            if (!file.isEmpty()) {
                resources.add(store.putResource(metadataUuid, file, visibility));
            }
        }
        return resources;
    }

    @ApiOperation(value = "Create a new resource from a URL for a given metadata",
                  nickname = "putResourcesFromURL")
    @RequestMapping(method = RequestMethod.PUT)
    @PreAuthorize("hasRole('Editor')")
    @ResponseBody
    public List<MetadataResource> putResourceFromURL(
                            @ApiParam(value = "The metadata UUID",
                                      required = true,
                                      example = "43d7c186-2187-4bcd-8843-41e575a5ef56")
                            @PathVariable
                            String metadataUuid,
                            @ApiParam(value = "The sharing policy",
                                      example = "public")
                            @RequestParam(required = false,
                                          defaultValue = "public")
                            MetadataResourceVisibility visibility,
                            @ApiParam(value = "The URL to load in the store")
                            @RequestParam("url")
                            List<URL> urls)
            throws Exception {
        List<MetadataResource> resources = new ArrayList<>();
        for(URL url : urls) {
            resources.add(store.putResource(metadataUuid, url, visibility));
        }
        return resources;
    }


    @ApiOperation(value = "Get a metadata resource",
                  nickname = "getResource")
    @RequestMapping(value = "/{resourceId:.+}",
                    method = RequestMethod.GET)
    @ResponseBody
    public HttpEntity<byte[]> getResource(
                                @ApiParam(value = "The metadata UUID",
                                          required = true,
                                          example = "43d7c186-2187-4bcd-8843-41e575a5ef56")
                                @PathVariable
                                String metadataUuid,
                                @ApiParam(value = "The resource identifier (ie. filename)",
                                          required = true)
                                @PathVariable
                                String resourceId
        ) throws Exception {
        Path file = store.getResource(metadataUuid, resourceId);

        // TODO: Check user privileges

        MultiValueMap<String, String> headers = new HttpHeaders();
        headers.add("Content-Disposition",
                    "inline; filename=\"" + file.getFileName() + "\"");
        headers.add("Cache-Control",
                    "no-cache");
        headers.add("Content-Type", getFileContentType(file));

        return new HttpEntity<>(Files.readAllBytes(file), headers);
    }




    @ApiOperation(value = "Update the metadata resource visibility",
                  nickname = "patchMetadataResourceVisibility")
    @RequestMapping(value = "/{resourceId:.+}",
                    method = RequestMethod.PATCH)
    @PreAuthorize("hasRole('Editor')")
    @ResponseBody
    public MetadataResource patchResource(@ApiParam(value = "The metadata UUID",
                                            required = true,
                                            example = "43d7c186-2187-4bcd-8843-41e575a5ef56")
                                  @PathVariable
                                  String metadataUuid,
                                  @ApiParam(value = "The resource identifier (ie. filename)",
                                            required = true)
                                  @PathVariable
                                  String resourceId,
                                  @ApiParam(value = "The visibility",
                                            required = true,
                                            example = "public")
                                  @RequestParam(required = true)
                                  MetadataResourceVisibility visibility) throws Exception {
        return store.patchResourceStatus(metadataUuid, resourceId, visibility);
    }



    @ApiOperation(value = "Delete a metadata resource",
                  nickname = "deleteMetadataResource")
    @RequestMapping(value = "/{resourceId:.+}",
                    method = RequestMethod.DELETE)
    @PreAuthorize("hasRole('Editor')")
    @ResponseBody
    public boolean delResource(@ApiParam(value = "The metadata UUID",
                                         required = true,
                                         example = "43d7c186-2187-4bcd-8843-41e575a5ef56")
                               @PathVariable
                               String metadataUuid,
                               @ApiParam(value = "The resource identifier (ie. filename)",
                                         required = true)
                               @PathVariable
                               String resourceId) throws Exception {
        store.delResource(metadataUuid, resourceId);
        return true;
    }




    /**
     * Based on the file content or file extension return
     * an appropiate mime type.
     *
     * @param file
     * @return  The mime type or application/{{file_extension}} if none found.
     * @throws IOException
     */
    public static String getFileContentType(Path file) throws IOException {
        String contentType = Files.probeContentType(file);
        if (contentType == null) {
            String ext = com.google.common.io.Files.getFileExtension(file.getFileName().toString()).toLowerCase();
            switch (ext) {
                case "png":
                case "gif":
                case "bmp":
                case "tif":
                case "tiff":
                case "jpg":
                case "jpeg":
                    contentType = "image/" + ext;
                    break;
                case "txt":
                case "html":
                    contentType = "text/" + ext;
                    break;
                default:
                    contentType = "application/" + ext;
            }
        }
        return contentType;
    }




    // TODO: i18n
    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({
            MaxUploadSizeExceededException.class,
            FileNotFoundException.class})
    public Object fileNotFoundHandler(final Exception exception) {
        exception.printStackTrace();
        return  new HashMap<String, String>() {{
            put("result", "failed");
            put("type", "file_not_found");
            put("message", exception.getClass() + " " + exception.getMessage());
        }};
    }
    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Object missingParameterHandler(final Exception exception) {
        return  new HashMap<String, String>() {{
            put("result", "failed");
            put("type", "required_parameter_missing");
            put("message", exception.getMessage());
        }};
    }
    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({
            UnsatisfiedServletRequestParameterException.class,
            IllegalArgumentException.class
    })
    public Object unsatisfiedParameterHandler(final Exception exception) {
        return  new HashMap<String, String>() {{
            put("result", "failed");
            put("type", "unsatisfied_request_parameter");
            put("message", exception.getMessage());
        }};
    }
}
