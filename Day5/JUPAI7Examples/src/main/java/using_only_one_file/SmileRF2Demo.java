package using_only_one_file;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.csv.CSVFormat;
import smile.classification.RandomForest;
import smile.data.DataFrame;
import smile.data.formula.Formula;
import smile.data.measure.NominalScale;
import smile.data.vector.IntVector;
import smile.data.vector.StringVector;
import smile.io.Read;
import smile.plot.swing.Histogram;
import usingClasses.PassengerProvider2;
import usingClasses.SmileDemoEDA;

public class SmileRF2Demo {

    public static int[] encodeCategory(DataFrame df, String columnName) {
        String[] values = df.stringVector(columnName).distinct().toArray(new String[] {});
        int[] pclassValues = df.stringVector(columnName).factorize(new NominalScale(values)).toIntArray();
        return pclassValues;
    }
    public static void main(String[] args) throws InvocationTargetException, InterruptedException, IOException, URISyntaxException {
        DataFrame titanic = Read.csv("src/main/resources/data/titanic_train.csv", CSVFormat.DEFAULT.withDelimiter(',')
        		.withHeader("PassengerId","Survived","Pclass","Name","Sex","Age")
                .withSkipHeaderRecord(true));
        
        titanic=titanic.drop("PassengerId");

       titanic = titanic.merge(IntVector.of("PclassValue", encodeCategory(titanic, "Pclass")));
        titanic = titanic.merge(IntVector.of("SexValue", encodeCategory(titanic, "Sex")));
        eda(titanic);
        titanic = titanic.drop("Name");
        titanic = titanic.drop("Sex");
        titanic = titanic.drop("Pclass");
        titanic = titanic.omitNullRows();
        System.out.println(titanic.schema());
        System.out.println(titanic.summary());
        RandomForest model = RandomForest.fit(Formula.lhs("Survived"), titanic);
        System.out.println("feature importance:");
        System.out.println(Arrays.toString(model.importance()));
        System.out.println(model.metrics ());
        
     // Testing phase
        System.out.println ("=======Entering test phase ==============");

        DataFrame testData = Read.csv("src/main/resources/data/titanic_test.csv", CSVFormat.DEFAULT.withDelimiter(',')
                .withHeader("PassengerId","Pclass","Name","Sex","Age")
                .withSkipHeaderRecord(true));
        
        testData=testData.drop("PassengerId");
        testData=testData.drop("Name");

        System.out.println ("1>>>>>>"+testData.structure ());
        System.in.read();
        System.out.println ("2>>>>>>"+testData.summary ());
         System.in.read();
         System.out.println ("=======Encoding Non Numeric Data==============");

         testData = testData.merge (IntVector.of ("Gender",
                encodeCategory (testData, "Sex")));
         

         testData = testData.merge (StringVector.of ("classes_string",testData.intVector("Pclass").toStringArray()));
         testData=testData.drop("Pclass");

         testData = testData.merge (IntVector.of ("PClassValues",encodeCategory (testData, "classes_string")));
         
         System.out.println (testData.structure ());
         System.in.read();
        System.out.println ("=======Dropping  classes_string, and Sex Columns==============");
        testData=testData.drop("Sex");
        testData=testData.drop("classes_string");

        System.out.println (testData.structure ());
         System.in.read();
        System.out.println (testData.summary ());
         System.in.read();
         testData = testData.omitNullRows ();
        System.out.println ("=======After Omitting null Rows==============");
        System.out.println (testData.summary ());
         System.in.read();
         
        testData = testData.merge (IntVector.of ("Survived",model.predict(testData)));
        
        System.out.println (testData.structure ());
        System.in.read();
       System.out.println (testData.summary ());
       System.out.println (testData);
       
    }
    private static void eda(DataFrame titanic) throws InterruptedException, InvocationTargetException {
        titanic.summary();
        DataFrame titanicSurvived = DataFrame.of(titanic.stream().filter(t -> t.get("Survived").equals(1)));
        DataFrame titanicNotSurvived = DataFrame.of(titanic.stream().filter(t -> t.get("Survived").equals(0)));
        titanicNotSurvived.omitNullRows().summary();
        titanicSurvived = titanicSurvived.omitNullRows();
        titanicSurvived.summary();
        int size = titanicSurvived.size();
        System.out.println(size);
        Double averageAge = titanicSurvived.stream()
                .mapToDouble(t -> t.isNullAt("Age" ) ? 0.0 : t.getDouble("Age"))
                .average()
                .orElse(0);
        System.out.println(averageAge.intValue());
        Map map = titanicSurvived.stream()
                .collect(Collectors.groupingBy(t -> Double.valueOf(t.getDouble("Age")).intValue(), Collectors.counting()));

        double[] breaks = ((Collection<Integer>)map.keySet())
                .stream()
                .mapToDouble(l -> Double.valueOf(l))
                .toArray();

        int[] valuesInt = ((Collection<Long>) map.values())
                .stream().mapToInt(i -> i.intValue())
                .toArray();

//    Histogram.of(titanicSurvived.doubleVector("Age").toDoubleArray(), values.length, true)
//          .canvas().setAxisLabels("Age","Count")
//          .setTitle("Age frequencies among surviving passengers" )
//          .window();
        Histogram.of(titanicSurvived.intVector("PclassValue").toIntArray(),4, true)
                .canvas().setAxisLabels("Classes","Count")
                .setTitle("Pclass values frequencies among surviving passengers" )
                .window();
        //Histogram.of(values, map.size(), false).canvas().window();
        titanicSurvived.schema();
    }
}