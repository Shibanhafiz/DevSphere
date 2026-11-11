package com.devsphere.service;

import com.devsphere.dao.ProjectFileDao;
import com.devsphere.model.ProjectFile;
import com.devsphere.model.Session;

import java.io.IOException;
import java.nio.file.*;
import java.sql.SQLException;
import java.util.*;

public class ProjectFileService {
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf","png","jpg","jpeg","gif","bmp","txt","md","csv","java","xml","json","sql","yml","yaml","properties","doc","docx","xls","xlsx","ppt","pptx","zip");
    private final ProjectFileDao dao;
    private final ProjectService projectService;
    private final Path storageRoot;
    private final long maxBytes;

    public ProjectFileService(ProjectFileDao dao, ProjectService projectService){this.dao=dao;this.projectService=projectService;this.storageRoot=Paths.get(env("DEVSPHERE_STORAGE_DIR","devsphere-storage")).toAbsolutePath().normalize();this.maxBytes=parseLimit(env("DEVSPHERE_MAX_FILE_SIZE_MB","25"));}
    public List<ProjectFile> list(Session s,long projectId)throws SQLException,ProjectAccessException{projectService.getProjectForUser(s,projectId);return dao.findForProject(projectId);}
    public ProjectFile upload(Session s,long projectId,Path source,String description)throws Exception{projectService.getProjectForUser(s,projectId);if(!Files.isRegularFile(source))throw new IllegalArgumentException("Choose a regular file.");String original=validatedName(source.getFileName().toString());long size=Files.size(source);validateSize(size);String ext=extension(original);Path dir=projectDir(projectId);Files.createDirectories(dir);String stored=UUID.randomUUID()+"."+(ext.isBlank()?"bin":ext);Path target=dir.resolve(stored);Files.copy(source,target);try{long id=dao.insert(projectId,original,stored,relative(target),size,ext,description,s.userId());return dao.find(id,projectId);}catch(Exception e){Files.deleteIfExists(target);throw e;}}
    public Path download(Session s,ProjectFile file,Path destination)throws Exception{projectService.getProjectForUser(s,file.projectId());Path source=safeStoredPath(file);Path output=destination.toAbsolutePath().normalize();if(Files.isDirectory(output))output=output.resolve(file.originalName());Files.copy(source,output,StandardCopyOption.REPLACE_EXISTING);return output;}
    public void renameOrDescribe(Session s,ProjectFile file,String newName,String description)throws Exception{requireModify(s,file);String name=validatedName(newName);dao.updateMetadata(file.id(),file.projectId(),name,description);}
    public ProjectFile replace(Session s,ProjectFile file,Path source)throws Exception{requireModify(s,file);String original=validatedName(source.getFileName().toString());long size=Files.size(source);validateSize(size);String ext=extension(original);Path target=safeStoredPath(file);Path temp=target.resolveSibling(UUID.randomUUID()+".uploading");Files.copy(source,temp);Path backup=target.resolveSibling(file.storedName()+".backup");try{Files.move(target,backup,StandardCopyOption.REPLACE_EXISTING);Files.move(temp,target,StandardCopyOption.REPLACE_EXISTING);dao.replaceMetadata(file.id(),file.projectId(),original,file.storedName(),file.relativePath(),size,ext);Files.deleteIfExists(backup);return dao.find(file.id(),file.projectId());}catch(Exception e){Files.deleteIfExists(temp);Files.deleteIfExists(target);if(Files.exists(backup))Files.move(backup,target,StandardCopyOption.REPLACE_EXISTING);throw e;}}
    public void delete(Session s,ProjectFile file)throws Exception{requireModify(s,file);dao.delete(file.id(),file.projectId());try{Files.deleteIfExists(safeStoredPath(file));}catch(IOException e){throw new IOException("File metadata was removed, but the physical file could not be removed: "+e.getMessage(),e);}}
    public void deleteProjectFiles(long projectId)throws IOException{Path dir=projectDir(projectId);if(!Files.exists(dir))return;IOException failure=null;try(var paths=Files.walk(dir)){for(Path p:(Iterable<Path>)paths.sorted(Comparator.reverseOrder())::iterator){try{Files.deleteIfExists(p);}catch(IOException e){failure=e;}}}if(failure!=null)throw failure;}
    private void requireModify(Session s,ProjectFile f)throws Exception{var p=projectService.getProjectForUser(s,f.projectId());if(p.ownerId()!=s.userId()&&f.uploadedBy()!=s.userId())throw new ProjectAccessException("Only the project owner or uploader can modify this file.");}
    private Path safeStoredPath(ProjectFile f){Path path=storageRoot.resolve(f.relativePath()).normalize();if(!path.startsWith(storageRoot))throw new SecurityException("Unsafe file path.");return path;}
    private Path projectDir(long id){return storageRoot.resolve("projects").resolve(Long.toString(id)).resolve("uploaded-files").normalize();}
    private String relative(Path path){return storageRoot.relativize(path).toString().replace('\\','/');}
    private String validatedName(String n){if(n==null||n.isBlank()||n.length()>255||n.contains("/")||n.contains("\\")||n.contains("..")||n.chars().anyMatch(Character::isISOControl))throw new IllegalArgumentException("Filename is invalid.");return n.trim();}
    private String extension(String n){int dot=n.lastIndexOf('.');String ext=dot<0?"":n.substring(dot+1).toLowerCase(Locale.ROOT);if(!ext.isBlank()&&!ALLOWED_EXTENSIONS.contains(ext))throw new IllegalArgumentException("This file type is not supported.");return ext;}
    private void validateSize(long size){if(size<0||size>maxBytes)throw new IllegalArgumentException("File exceeds the configured limit of "+(maxBytes/(1024*1024))+" MB.");}
    private long parseLimit(String value){try{long mb=Long.parseLong(value);if(mb<1||mb>1024)throw new NumberFormatException();return mb*1024L*1024L;}catch(NumberFormatException e){return 25L*1024L*1024L;}}
    private String env(String n,String d){String v=System.getenv(n);return v==null||v.isBlank()?d:v;}
}
