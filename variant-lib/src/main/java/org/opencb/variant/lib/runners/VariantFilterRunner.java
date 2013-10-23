package org.opencb.variant.lib.runners;

import org.opencb.commons.bioformats.commons.filters.FilterApplicator;
import org.opencb.commons.bioformats.variant.vcf4.VcfRecord;
import org.opencb.commons.bioformats.variant.vcf4.filters.VcfFilter;
import org.opencb.commons.bioformats.variant.vcf4.io.readers.VariantDataReader;
import org.opencb.commons.bioformats.variant.vcf4.io.readers.VariantVcfDataReader;
import org.opencb.commons.bioformats.variant.vcf4.io.writers.vcf.VariantDataWriter;
import org.opencb.commons.bioformats.variant.vcf4.io.writers.vcf.VariantVcfDataWriter;
import org.opencb.javalibs.commons.containers.DataItem;
import org.opencb.javalibs.commons.containers.DataRW;

import java.util.List;
import java.util.concurrent.*;

/**
 * Created with IntelliJ IDEA.
 * User: aleman
 * Date: 9/13/13
 * Time: 8:01 PM
 * To change this template use File | Settings | File Templates.
 */
public class VariantFilterRunner {

    private VariantDataReader vcfReader;
    private VariantDataWriter vcfWriter;
    private List<VcfFilter> filters;
    private int batchSize = 1000;
    private int threads;


    public VariantFilterRunner() {
        this.threads = 2;
    }

    public VariantFilterRunner(VariantDataReader vcfReader, VariantDataWriter vcfWriter) {
        this();
        this.vcfReader = vcfReader;
        this.vcfWriter = vcfWriter;
    }

    public VariantFilterRunner(String vcfFileName, String vcfOutFilename) {
        this(new VariantVcfDataReader(vcfFileName), new VariantVcfDataWriter(vcfOutFilename));
    }

    public VariantFilterRunner parallel(int numThreads) {
        this.threads = numThreads;
        return this;
    }

    public void run() {

        int cont = 1;

        vcfReader.open();
        vcfWriter.open();

        vcfReader.pre();
        vcfWriter.pre();


        DataRW<List<VcfRecord>, List<VcfRecord>> data = new DataRW<>();

        ExecutorService threadPool = Executors.newFixedThreadPool(2 + this.threads);

        for (int i = 0; i < this.threads; i++) {
            threadPool.execute(new Filter(data));
        }

        Future producerStatus = threadPool.submit(new Reader(data));
        Future writer = threadPool.submit(new Writer(data));

        try {
            producerStatus.get();
            writer.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        threadPool.shutdown();

        vcfReader.post();
        vcfWriter.post();

        vcfReader.close();
        vcfWriter.close();
    }

    public void filters(List<VcfFilter> filters) {
        this.filters = filters;
    }

    public VariantFilterRunner reader(VariantDataReader reader) {
        this.vcfReader = reader;
        return this;
    }

    public VariantFilterRunner writer(VariantDataWriter writer) {
        this.vcfWriter = writer;
        return this;
    }

    private class Filter implements Runnable {

        private DataRW<List<VcfRecord>, List<VcfRecord>> data;

        public Filter(DataRW data) {
            this.data = data;
            this.data.incConsumer();
        }

        @Override
        public void run() {
            System.out.println("START FILTER");
            DataItem<List<VcfRecord>> dataItem = data.getRead();
            List<VcfRecord> batch;
            List<VcfRecord> batchAux;
            int priority;

            while (data.isContinueProducing() || dataItem != null) {
                batch = dataItem.getData();
                priority = dataItem.getTokenId();

                batchAux = FilterApplicator.filter(batch, filters);
                batch.clear();

                if (batchAux.size() > 0) {
                    data.putWrite(priority, batchAux);
                }

                dataItem = data.getRead();
            }

            System.out.println("END FILTER");
            this.data.decConsumer();
        }
    }

    private class Reader implements Runnable {

        private DataRW<List<VcfRecord>, List<VcfRecord>> data;
        private int count;

        private Reader(DataRW data) {
            this.data = data;
            this.count = 0;
        }

        @Override
        public void run() {
            List<VcfRecord> batch;

            batch = vcfReader.read(batchSize);

            System.out.println("START READER");
            vcfWriter.writeVcfHeader(vcfReader.getHeader());
            while (!batch.isEmpty()) {
                data.putRead(count++, batch);
                batch = vcfReader.read(batchSize);
            }

            data.setContinueProducing(false);

            System.out.println("END READER");

        }
    }

    private class Writer implements Runnable {

        private DataRW<List<VcfRecord>, List<VcfRecord>> data;

        private Writer(DataRW data) {
            this.data = data;
        }

        @Override
        public void run() {

            System.out.println("START WRITER");

            DataItem<List<VcfRecord>> dataItem;
            List<VcfRecord> batch;
            dataItem = data.getWrite();

            while (dataItem != null) {
                batch = dataItem.getData();
                System.out.println("Writing: " + dataItem.getTokenId() + " size: " + batch.size());
                vcfWriter.writeBatch(batch);
                batch.clear();
                dataItem = data.getWrite();
            }
            System.out.println("END WRITER");
        }
    }
}
