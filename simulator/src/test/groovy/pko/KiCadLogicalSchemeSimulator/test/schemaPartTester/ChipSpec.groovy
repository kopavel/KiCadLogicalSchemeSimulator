package pko.KiCadLogicalSchemeSimulator.test.schemaPartTester

import groovy.transform.DefaultsMode
import groovy.transform.TupleConstructor
import pko.KiCadLogicalSchemeSimulator.Simulator
import pko.KiCadLogicalSchemeSimulator.api.IModelItem
import pko.KiCadLogicalSchemeSimulator.api.bus.OutBus
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPart
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.SchemaPartSpi
import pko.KiCadLogicalSchemeSimulator.api.wire.OutPin
import spock.lang.Specification

abstract class ChipSpec extends Specification {

    @TupleConstructor(defaultsMode = DefaultsMode.AUTO)
    static final class ChipDefinition {
        final SchemaPart part
        final SchemaPartSpi spi
        final List<String> inputs
        final List<String> outputs
        final boolean optimise = true
    }

    SchemaPart part
    IModelItem<?>[] ins
    IModelItem<?>[] out

    protected abstract ChipDefinition chip()

    def setup() {
        ChipDefinition definition = chip()

        part = definition.part

        Simulator.schemaPartSpiMap = [
                "SPI": definition.spi
        ]

        out = definition.outputs.collect { String name ->
            IModelItem<?> source = part.getOutItem(name)

            IModelItem<?> tester
            if (source instanceof OutPin) {
                tester = new TesterInPin(name)
            } else if (source instanceof OutBus) {
                tester = new TesterInBus(name, source.size)
            } else {
                throw new IllegalArgumentException(
                        "Unsupported output ${name}: ${source.class.name}"
                )
            }

            source.addDestination(tester)
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
    }
}