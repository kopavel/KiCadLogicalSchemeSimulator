package pko.KiCadLogicalSchemeSimulator.test.schemaPartTester

import groovy.transform.DefaultsMode
import groovy.transform.TupleConstructor
import pko.KiCadLogicalSchemeSimulator.Simulator
import pko.KiCadLogicalSchemeSimulator.api.IModelItem
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus
import pko.KiCadLogicalSchemeSimulator.api.bus.OutBus
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPartSpi
import pko.KiCadLogicalSchemeSimulator.api.wire.OutPin
import pko.KiCadLogicalSchemeSimulator.optimiser.ClassOptimiser
import pko.KiCadLogicalSchemeSimulator.tools.Utils
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Stepwise

@Stepwise
abstract class ChipSpec extends Specification {

    @TupleConstructor(defaultsMode = DefaultsMode.AUTO)
    static final class ChipDefinition {
        final SchemaPartSpi spi
        final String params
        final List<String> inputs
        final List<String> outputs
        final boolean optimise = true
        final boolean shared = true
    }

    @Shared
    SchemaPart part
    @Shared
    IModelItem<?>[] ins
    @Shared
    IModelItem<?>[] out

    protected abstract ChipDefinition chip()

    def setup() {
        ChipDefinition definition = chip()
        if (part == null || !definition.shared) {
            init(definition)
        }
    }

    def init(ChipDefinition definition) {
        ClassOptimiser.force = true;
        Simulator.optimisedDir = "../../simulator/optimised";

        part = definition.spi.getSchemaPart("chip", definition.params);

        Simulator.schemaPartSpiMap = [
                "SPI": definition.spi
        ]

        out = definition.outputs.collect { String name ->
            IModelItem<?> source = part.getOutItem(name)

            IModelItem<?> tester
            if (source instanceof OutPin) {
                tester = new TesterInPin(name)
                source.addDestination(tester)
            } else if (source instanceof OutBus) {
                tester = new TesterInBus(name, source.size)
                source.addDestination((Bus) tester, Utils.getMaskForSize(source.size), (byte) 0)
            } else {
                throw new IllegalArgumentException(
                        "Unsupported output ${name}: ${source.class.name}"
                )
            }

            tester
        } as IModelItem<?>[]

        ins = definition.inputs.collect { String name ->
            IModelItem<?> inItem = part.getInItem(name)
            definition.optimise ? inItem.getOptimised(null) : inItem
        } as IModelItem<?>[]

        if (definition.optimise)
            part.optimiseOuts()
        else {
            part.initOuts()
            part.outPins.values().forEach { i -> i.resend() }
        }
        part.reset()
    }

    protected void setInputs(Object... states) {
        assert states.length == ins.length

        states.eachWithIndex { state, i ->
            ins[i].syncState(state as int)
        }
    }

    protected void checkOutputs(Object... expected) {
        assert expected.length == out.length

        expected.eachWithIndex { value, i ->
            if (value == 'h') {
                assert out[i].hiImpedance
            } else {
                assert !out[i].hiImpedance
                assert out[i].getState() == value
            }
        }
    }
}