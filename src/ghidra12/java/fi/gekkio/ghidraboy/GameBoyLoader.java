package fi.gekkio.ghidraboy;

import ghidra.app.util.Option;
import ghidra.app.util.bin.ByteProvider;
import ghidra.app.util.opinion.*;
import ghidra.framework.model.DomainObject;
import ghidra.program.model.listing.Program;
import ghidra.util.exception.CancelledException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GameBoyLoader extends CommonGameBoyLoader {
    @Override
    public List<Option> getDefaultOptions(ByteProvider provider, LoadSpec loadSpec, DomainObject domainObject, boolean isLoadIntoProgram, boolean mirrorFsLayout) {
        var result = super.getDefaultOptions(provider, loadSpec, domainObject, isLoadIntoProgram, mirrorFsLayout);
        return commonGameBoyGetDefaultOptions(provider, result);
    }

    @Override
    protected List<Loaded<Program>> loadProgram(ImporterSettings settings) throws IOException, LoadException, CancelledException {
        var result = new ArrayList<Loaded<Program>>();
        var log = settings.log();
        var options = settings.options();

        var program = createProgram(settings);
        var success = false;

        try {
            commonGameBoyAddDataTypes(program, options);

            loadInto(program, settings);
            createDefaultMemoryBlocks(program, settings);

            commonGameBoyAddHardwareBlocks(program, options, log);

            success = result.add(new Loaded<>(program, settings));
        } finally {
            if (!success) {
                program.release(settings.consumer());
            }
        }
        return result;    
    }

    @Override
    protected void loadProgramInto(Program program, ImporterSettings settings) throws IOException, LoadException, CancelledException {
        var log = settings.log();
        var monitor = settings.monitor();
        var options = settings.options();
        var provider = settings.provider();

        commonGameBoyLoadProgramInto(provider, options, log, program, monitor);
    }
}
