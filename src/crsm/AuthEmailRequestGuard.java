package crsm;

import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/** Shared email-request guard for JavaFX, Swing and direct service callers; never retries. */
final class AuthEmailRequestGuard {
    private static final Map<String,AuthEmailRequestGuard> PROJECTS=new ConcurrentHashMap<>();
    private final LongSupplier clock;
    private final Path stateFile;
    private long blockedUntil;
    private int limitedResponses;
    private boolean inFlight;

    static AuthEmailRequestGuard forProject(String project){
        return PROJECTS.computeIfAbsent(project==null?"":project,key->{
            Path state=null;
            try{
                String host=java.net.URI.create(key).getHost();
                if(host!=null&&!Set.of("localhost","127.0.0.1","::1").contains(host)){
                    String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8))).substring(0,24);
                    state=Path.of(System.getProperty("user.dir","."),".crsm-local","email-pause-"+hash+".properties");
                }
            }catch(Exception ignored){}
            return new AuthEmailRequestGuard(System::currentTimeMillis,state);
        });
    }
    AuthEmailRequestGuard(LongSupplier clock){this(clock,null);}
    AuthEmailRequestGuard(LongSupplier clock,Path stateFile){this.clock=clock;this.stateFile=stateFile;load();}
    synchronized long remainingSeconds(){load();return Math.max(0,(blockedUntil-clock.getAsLong()+999)/1000);}
    synchronized void begin(){
        long remaining=remainingSeconds();
        if(inFlight)throw new IllegalStateException("An email request is already in progress. No additional request was sent.");
        if(remaining>0)throw new IllegalStateException("Email requests are paused in this app for "+remaining+" seconds. Check your inbox first. The provider limit may last longer; no additional request was sent.");
        inFlight=true;blockedUntil=clock.getAsLong()+60000;save();
    }
    synchronized void complete(AuthRateLimitException limit,boolean accepted){
        if(limit!=null){
            limitedResponses=Math.min(limitedResponses+1,7);
            long seconds=limit.retryAfterSeconds().orElse(Math.min(3600,60L<<(limitedResponses-1)));
            blockedUntil=Math.max(blockedUntil,clock.getAsLong()+Math.max(1,seconds)*1000);
        }else if(accepted){limitedResponses=0;}
        inFlight=false;save();
    }
    private void load(){
        if(stateFile==null)return;
        try{
            Properties props=new Properties();try(var input=Files.newInputStream(stateFile)){props.load(input);}
            long deadline=Long.parseLong(props.getProperty("retry_not_before_ms","0"));
            // A corrupt state file must not impose a permanent lock.
            if(deadline<=clock.getAsLong()+604800000L)blockedUntil=Math.max(blockedUntil,deadline);
            limitedResponses=Math.max(limitedResponses,Math.min(7,Integer.parseInt(props.getProperty("limited_responses","0"))));
        }catch(java.io.IOException|NumberFormatException ignored){}
    }
    private void save(){
        if(stateFile==null)return;
        try{
            Files.createDirectories(stateFile.getParent());
            Properties props=new Properties();props.setProperty("retry_not_before_ms",Long.toString(blockedUntil));props.setProperty("limited_responses",Integer.toString(limitedResponses));
            Path temp=Files.createTempFile(stateFile.getParent(),"email-pause-",".tmp");
            try{try(var output=Files.newOutputStream(temp)){props.store(output,"Local retry timing only; no account details or credentials");}Files.move(temp,stateFile,StandardCopyOption.REPLACE_EXISTING);}
            finally{Files.deleteIfExists(temp);}
        }catch(java.io.IOException ignored){/* In-memory guarding remains active when local state is not writable. */}
    }
}
