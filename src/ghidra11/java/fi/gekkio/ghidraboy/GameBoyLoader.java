package fi.gekkio.ghidraboy;

import ghidra.app.util.Option;
import ghidra.app.util.bin.ByteProvider;
import ghidra.app.util.importer.MessageLog;
import ghidra.app.util.opinion.*;
import ghidra.framework.model.DomainObject;
import ghidra.framework.model.Project;
import ghidra.program.model.listing.Program;
import ghidra.util.exception.CancelledException;
import ghidra.util.task.TaskMonitor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GameBoyLoader extends CommonGameBoyLoader {
    @Override
    public List<Option> getDefaultOptions(ByteProvider provider, LoadSpec loadSpec, DomainObject domainObject, boolean isLoadIntoProgram) {
        var result = super.getDefaultOptions(provider, loadSpec, domainObject, isLoadIntoProgram);
        return commonGameBoyGetDefaultOptions(provider, result);
    }

    @Override
    protected List<Loaded<Program>> loadProgram(ByteProvider provider, String loadedName, Project project, String projectFolderPath, LoadSpec loadSpec, List<Option> options, MessageLog log, Object consumer, TaskMonitor monitor) throws IOException, CancelledException {
        var result = new ArrayList<Loaded<Program>>();
        var pair = loadSpec.getLanguageCompilerSpec();
        var language = getLanguageService().getLanguage(pair.languageID);
        var compiler = language.getCompilerSpecByID(pair.compilerSpecID);

        var baseAddress = language.getAddressFactory().getDefaultAddressSpace().getAddress(0);
        var program = createProgram(provider, loadedName, baseAddress, getName(), language, compiler, consumer);
        var success = false;
        try {
            commonGameBoyAddDataTypes(program, options);

            loadInto(provider, loadSpec, options, log, program, monitor);
            createDefaultMemoryBlocks(program, language, log);

            commonGameBoyAddHardwareBlocks(program, options, log);

            success = result.add(new Loaded<>(program, loadedName, projectFolderPath));
        } finally {
            if (!success) {
                program.release(consumer);
            }
        }
        return result; 
    }

    @Override
    protected void loadProgramInto(ByteProvider provider, LoadSpec loadSpec, List<Option> options, MessageLog log, Program program, TaskMonitor monitor) throws IOException, CancelledException {
        commonGameBoyLoadProgramInto(provider, options, log, program, monitor);
    }
}
