package org.opencb.variant.lib.io.variant.writers;

import org.opencb.variant.lib.core.formats.VcfRecord;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: aleman
 * Date: 9/15/13
 * Time: 3:40 PM
 * To change this template use File | Settings | File Templates.
 */
public class VariantVcfDataWriter implements VariantDataWriter {

    private PrintWriter printer;
    private String filename;


    public VariantVcfDataWriter(String filename) {
        this.filename = filename;
    }

    @Override
    public boolean open() {

        boolean res = true;
        try {
            printer = new PrintWriter(filename);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            res = false;
        }

        return res;
    }

    @Override
    public boolean close() {

        printer.close();

        return true;
    }

    @Override
    public boolean pre() {

        return true;
    }

    @Override
    public boolean post() {
        return true;
    }


    @Override
    public void writeVcfHeader(String header) {

        printer.append(header);

    }

    @Override
    public void writeBatch(List<VcfRecord> batch) {

        for(VcfRecord record: batch){
            printer.append(record.toString()).append("\n");
        }
    }
}
