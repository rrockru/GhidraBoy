package fi.gekkio.ghidraboy.decompiler

import fi.gekkio.ghidraboy.DataTypes.u16
import fi.gekkio.ghidraboy.DataTypes.u8
import fi.gekkio.ghidraboy.GameBoyKind
import fi.gekkio.ghidraboy.GameBoyUtils
import fi.gekkio.ghidraboy.IntegrationTest
import fi.gekkio.ghidraboy.withTransaction
import ghidra.app.decompiler.DecompInterface
import ghidra.app.plugin.assembler.Assemblers
import ghidra.app.util.importer.MessageLog
import ghidra.framework.Application
import ghidra.program.database.ProgramDB
import ghidra.program.model.address.Address
import ghidra.program.model.address.AddressSet
import ghidra.program.model.data.DataType
import ghidra.program.model.data.PointerDataType
import ghidra.program.model.lang.Register
import ghidra.program.model.listing.Function
import ghidra.program.model.listing.Instruction
import ghidra.program.model.listing.Parameter
import ghidra.program.model.listing.ParameterImpl
import ghidra.program.model.listing.Program
import ghidra.program.model.listing.ReturnParameterImpl
import ghidra.program.model.symbol.SourceType
import ghidra.util.task.TaskMonitor
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DecompilerTestGhidra11 : IntegrationTest() {
    private lateinit var program: Program
    private lateinit var decompiler: DecompInterface

    @Test
    fun `DAA decompilation`() {
        val f =
            assembleFunction(
                address(0x0000),
                """
                LD A, 0x01
                ADD C
                DAA
                LD C, A
                RET Z
                INC C
                RET
                """.trimIndent(),
                name = "daa",
                params =
                    listOf(
                        parameter("value", u8, register("C")),
                    ),
                returnParam = returnParameter(u8, register("C")),
            )
        assertDecompiled(
            f,
            """
            byte daa(byte value)
            {
                char cVar1;
                byte bVar2;
                cVar1 = daaOperand(value + 1,0xfe < value,((value & 0xf) + 1 & 0x10) != 0,0);
                bVar2 = value + 1 + cVar1;
                if (bVar2 == 0) {
                    return bVar2;
                }
                return bVar2 + 1;
            }
            """.trimIndent(),
        )
    }

    @BeforeAll
    override fun beforeAll() {
        super.beforeAll()
        decompiler = DecompInterface()
    }

    @BeforeEach
    fun beforeEach() {
        val consumer = object {}
        program = ProgramDB("test", language, language.defaultCompilerSpec, consumer)
        program.withTransaction {
            program.memory.createInitializedBlock("rom", address(0x0000), 0x8000, 0, TaskMonitor.DUMMY, false)
            GameBoyUtils.addHardwareBlocks(program, GameBoyKind.CGB, MessageLog())
            GameBoyUtils.populateHardwareBlocks(program, GameBoyKind.CGB)
        }
        assertTrue(decompiler.openProgram(program)) { "Failed to initialize decompiler" }
    }

    @AfterEach
    fun afterEach() {
        decompiler.closeProgram()
    }

    @AfterAll
    fun afterAll() {
        decompiler.dispose()
    }

    private fun assembleFunction(
        address: Address,
        code: String,
        name: String? = null,
        params: List<Parameter>? = null,
        returnParam: Parameter? = null,
    ): Function =
        program.withTransaction {
            val instructions: Iterable<Instruction> =
                Assemblers.getAssembler(program).assemble(address, *code.lines().toTypedArray())
            val addressSet = AddressSet()
            for (instruction in instructions) {
                addressSet.add(instruction.minAddress, instruction.maxAddress)
            }
            program.functionManager.createFunction(name, address, addressSet, SourceType.USER_DEFINED).apply {
                setCustomVariableStorage(true)
                val callingConvention = "default"
                val force = true
                if (params != null) {
                    updateFunction(
                        callingConvention,
                        returnParam,
                        params,
                        Function.FunctionUpdateType.CUSTOM_STORAGE,
                        force,
                        SourceType.USER_DEFINED,
                    )
                } else {
                    updateFunction(
                        callingConvention,
                        returnParam,
                        Function.FunctionUpdateType.CUSTOM_STORAGE,
                        force,
                        SourceType.USER_DEFINED,
                    )
                }
            }
        }

    private fun decompile(function: Function) =
        decompiler
            .decompileFunction(function, 10, TaskMonitor.DUMMY)
            .also {
                assertTrue(it.decompileCompleted()) { "Decompilation did not complete" }
            }.decompiledFunction.c

    private fun formatCode(code: String) =
        code
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(separator = "\n")

    private fun assertDecompiled(
        function: Function,
        @Language("C") code: String,
    ) = assertEquals(formatCode(code), formatCode(decompile(function)))

    private fun parameter(
        name: String,
        type: DataType,
        register: Register,
    ) = ParameterImpl(name, type, register, program)

    private fun returnParameter(
        type: DataType,
        register: Register,
    ) = ReturnParameterImpl(type, register, program)

    private fun pointer(type: DataType) = PointerDataType(type)

    private fun register(name: String) = program.getRegister(name)
}
