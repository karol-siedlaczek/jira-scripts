File getAttachmentFile(Issue issue, Long attachmentId){
    def fileSystemAttachmentDirectoryAccessor = ComponentAccessor.getComponent(FileSystemAttachmentDirectoryAccessor.class)
    return fileSystemAttachmentDirectoryAccessor.getAttachmentDirectory(issue).listFiles().find({
        File it->
            it.getName().equals(attachmentId as String)
    })
}
