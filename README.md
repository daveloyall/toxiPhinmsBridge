# toxiPhinmsBridge
PHINMS transfers files. ToxiCALL/ToxiTRACK are compatible with SFTP, etc, but not PHINMS. This tool bridges the gap.

//TODO

## Usage
    //TODO
    
## Design

### Rough description of workflow on both sides

*NB: We'll call ToxiCALL at the Poison Control Center simply PCC Toxi.  We'll call ToxiTRACK at the Dept. of Health simply DOH Toxi.  This tool will be referred to as Bridge.*

1. Staff enters data into PCC Toxi; new records are created, they need to be sent to DOH.
1. PCC Toxi writes out three files to the Toxi incoming/outgoing directory. (The `S files`.)
1. PCC Bridge moves the `S files` from the PCC Toxi directory to the PCC PHINMS outgoing directory.
1. PCC PHINMS spots the `S files` and does the usual: moves them and transmits them.
1. Sometime later, the `S files` appear in DOH PHINMS incoming directory.
1. DOH Bridge moves those `S files` from the DOH PHINMS incoming directory to the DOH Toxi incoming/outgoing directory.
1. DOH Toxi picks 'em up, adds the data to the DOH Toxi DB, and writes out three files. (The `A files`, acknowledgement.)  PCC Toxi needs the files so it will know what not to send next time.
1. DOH Bridge moves the `A files` into the DOH PHINMS outgoing directory.
1. DOH PHINMS transmits those files to PCC PHINMS which writes them to the incoming directory.
1. PCC Bridge moves the `A files` from the PCC PHINMS Incoming directory to the PCC Toxi directory.
1. PCC Toxi reads in those files--now it knows which records DOH Toxi has.
1. Repeat.
