### 问题背景

一般我们在项目里都有很多json解析的代码，而在使用Gson框架解析json的时候，内部是使用反射来创建对象并设置属性的，如果json比较大，会有一定的解析耗时，解析对象可能在UI线程，或者说即使在子线程其实也会影响页面展现速度。

所以如果我们能找到一种低侵入式的方式将反射替换为new创建对象，就能够优化这一点。

### Gson反序列化对象的流程

我们先来看Gson反序列化的流程。





Gson反序列化的代码很简单：

```java
new Gson().fromJson(s, Person.class)
```

首先，在创建gson对象的时候，会往factories添加一些TypeAdapterFactory

```java
Gson(..) {
  ...
  factories.add(new ReflectiveTypeAdapterFactory(
      constructorConstructor, fieldNamingStrategy, excluder, jsonAdapterFactory));
	...
}
```

然后根据传入的json字符串，构件一个JsonReader对象：

```java
public <T> T fromJson(String json, Type typeOfT) throws JsonSyntaxException {
  if (json == null) {
    return null;
  }
  StringReader reader = new StringReader(json);
  T target = (T) fromJson(reader, typeOfT);
  return target;
}

  public <T> T fromJson(Reader json, Type typeOfT) throws JsonIOException, JsonSyntaxException {
    JsonReader jsonReader = newJsonReader(json);
    T object = (T) fromJson(jsonReader, typeOfT);
    assertFullConsumption(object, jsonReader);
    return object;
  }
  
  
```



然后根据type，获取一个TypeToken对象，然后获取到对应的typeAdapter，调用其的read方法来得到最终的解析对象：

```java
public <T> T fromJson(JsonReader reader, Type typeOfT) throws JsonIOException, JsonSyntaxException {
    ···
    try {
      reader.peek();
      isEmpty = false;
      TypeToken<T> typeToken = (TypeToken<T>) TypeToken.get(typeOfT);
      TypeAdapter<T> typeAdapter = getAdapter(typeToken);
      T object = typeAdapter.read(reader);
      return object;
    } 
  	···
  }
```

获取typeAdapter的逻辑如下：

```java
for (TypeAdapterFactory factory : factories) {
  TypeAdapter<T> candidate = factory.create(this, type);
  if (candidate != null) {
    call.setDelegate(candidate);
    typeTokenCache.put(type, candidate);
    return candidate;
  }
}
```

基本就是使用创建Gson对象的时候构建的factories，然后尝试用其的每一个factory来创建TypeAdapter，如果部位空，那么就使用这个TypeAdapter。

限免我们来看典型的TypeAdapter的read方法：

```java
ReflectiveTypeAdapterFactory：

public T read(JsonReader in) throws IOException {
  if (in.peek() == JsonToken.NULL) {
    in.nextNull();
    return null;
  }

  T instance = constructor.construct();

  try {
    in.beginObject();
    while (in.hasNext()) {
      String name = in.nextName();
      BoundField field = boundFields.get(name);
      if (field == null || !field.deserialized) {
        in.skipValue();
      } else {
        field.read(in, instance);
      }
    }
  } catch (IllegalStateException e) {
    throw new JsonSyntaxException(e);
  } catch (IllegalAccessException e) {
    throw new AssertionError(e);
  }
  in.endObject();
  return instance;
}
```

这里constructor.construct()就是创建对象的方法，里面有几种创建的策略，这里一般情况下会采用反射来创建对象。

field.read(in, instance)是给属性赋值的：

```java
void read(JsonReader reader, Object value) throws IOException, IllegalAccessException {
    Object fieldValue = mapped.read(reader);
    if (fieldValue != null || !isPrimitive) {
        field.set(value, fieldValue);
    }

}
```

这里会继续调用read方法来创建field的值，然后通过反射，将属性值赋给最终的对象，至此，解析对象完成。

### 使用new来替代反射

看了如上的反序列化流程之后，我们用new来替代反射的方法就已经很明显了，基本思路就是往gson里添加一个我们自定义的Factory，然后对某些我们支持的类型返回一个TypeAdapter，在这个read方法里来自行通过JsonReader对象来解析所需要的属性，然后通过new方法来创建我们的对象。

示例代码如下：

首先假设我们的对象如下：

```java
public class Person {
    public static final String TAG = "Person";
    public String name;
    public int age;
    public double height;
    public boolean isMan;
    public Address mAddress;
    public List<Phone> mPhoneList;

    public static class Address {
        public String detail;

        public Address(String detail) {
            Log.d(TAG, "Address: ");
            this.detail = detail;
        }

        @Override
        public String toString() {
            return "Address{" +
                    "detail='" + detail + '\'' +
                    '}';
        }
    }

    public static class Phone {
        public String number;

        public Phone(String number) {
            Log.d(TAG, "Phone: ");
            this.number = number;
        }

        @Override
        public String toString() {
            return "Phone{" +
                    "number='" + number + '\'' +
                    '}';
        }
    }

    public Person(String name, int age, double height, boolean isMan, Address address, List<Phone> phoneList) {
        this.name = name;
        this.age = age;
        this.height = height;
        this.isMan = isMan;
        mAddress = address;
        mPhoneList = phoneList;
        Log.d(TAG, "Person: ");
    }

    @Override
    public String toString() {
        return "Person{" +
                "name='" + name + '\'' +
                ", age=" + age +
                ", height=" + height +
                ", isMan=" + isMan +
                ", mAddress=" + mAddress +
                ", mPhoneList=" + mPhoneList +
                '}';
    }
}
```

针对这个对象，我们创建一个Factory如下：

```java
public class NoReflectTypeAdapterFactory implements TypeAdapterFactory {

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        if (Person.class.equals(type.getType())) {
            return new NoReflectPersonTypeAdapter<>(gson);
        }
        if (Person.Address.class.equals(type.getType())) {
            return new NoReflectAddressTypeAdapter<>(gson);
        }
        if (Person.Phone.class.equals(type.getType())) {
            return new NoReflectPhoneTypeAdapter<>(gson);
        }
        return null;
    }
}
```

之后，我们将这个Factory添加到gson里，那么针对我们指定的三种类型，就会使用我们自定义的TypeAdapter了，其中NoReflectPersonTypeAdapter如下：

```java
class NoReflectPersonTypeAdapter<T> extends TypeAdapter<T> {

    private Gson gson;

    public NoReflectPersonTypeAdapter(Gson gson) {
        this.gson = gson;
    }

    @Override
    public void write(JsonWriter out, T value) {

    }


    @Override
    public T read(JsonReader in) throws IOException {
        in.beginObject();
        String name = null;
        int age = 0;
        double height = 0;
        boolean isMan = false;
        Person.Address mAddress = null;
        List<Person.Phone> mPhoneList = null;
        while (in.hasNext()) {
            switch (in.nextName()) {
                case "name":
                    name = in.nextString();
                    break;
                case "age":
                    age = in.nextInt();
                    break;
                case "height":
                    height = in.nextDouble();
                    break;
                case "isMan":
                    isMan = in.nextBoolean();
                    break;
                case "mAddress":
                    mAddress = gson.getAdapter(new TypeToken<Person.Address>() {
                    }).read(in);
                    break;
                case "mPhoneList":
                    mPhoneList = gson.getAdapter(new TypeToken<List<Person.Phone>>() {
                    }).read(in);
                    break;
            }
        }
        in.endObject();
        return (T) new Person(name, age, height, isMan, mAddress, mPhoneList);
    }

    public static final String TAG = "Test";

}
```

可以看到，这里的基本实现就是先读取属性的名字，然后根据Person里各个属性的类型，调用JsonReader的对应方法来读取属性的值，最后在使用new创建对象。

测试代码如下：

```java
public static void test() {
    ArrayList<Person.Phone> phoneList = new ArrayList<>();
    phoneList.add(new Person.Phone("1234"));
    phoneList.add(new Person.Phone("5678"));
    Person person = new Person("xuyu", 20, 175.0, true, new Person.Address("南山"), phoneList);
    String s = new Gson().toJson(person);
    Log.d(TAG, "main: " + s);
    GsonBuilder gsonBuilder = new GsonBuilder().registerTypeAdapterFactory(new NoReflectTypeAdapterFactory());
    Log.d(TAG, "onCreate: " + gsonBuilder.create().fromJson(s, Person.class));
}
```



可以看到最终在反序列化过程中，有调用到构造方法：

```
2021-11-14 23:08:37.767 12472-12472/com.example.gsonoptimize D/Person: Phone: 
2021-11-14 23:08:37.767 12472-12472/com.example.gsonoptimize D/Person: Phone: 
2021-11-14 23:08:37.768 12472-12472/com.example.gsonoptimize D/Person: Address: 
2021-11-14 23:08:37.769 12472-12472/com.example.gsonoptimize D/Person: Person: 
2021-11-14 23:08:37.849 12472-12472/com.example.gsonoptimize D/Test: main: {"age":20,"height":175.0,"isMan":true,"mAddress":{"detail":"南山"},"mPhoneList":[{"number":"1234"},{"number":"5678"}],"name":"xuyu"}
2021-11-14 23:08:37.862 12472-12472/com.example.gsonoptimize D/Person: Address: 
2021-11-14 23:08:37.865 12472-12472/com.example.gsonoptimize D/Person: Phone: 
2021-11-14 23:08:37.865 12472-12472/com.example.gsonoptimize D/Person: Phone: 
2021-11-14 23:08:37.865 12472-12472/com.example.gsonoptimize D/Person: Person: 
2021-11-14 23:08:37.865 12472-12472/com.example.gsonoptimize D/Test: onCreate: Person{name='xuyu', age=20, height=175.0, isMan=true, mAddress=Address{detail='南山'}, mPhoneList=[Phone{number='1234'}, Phone{number='5678'}]}

```



去掉Phone类的答疑之后，做如下耗时测试：

```
public static void testTime() {
    ArrayList<Person.Phone> phoneList = new ArrayList<>();
    for (int i = 0; i < 1000000; i++) {
        phoneList.add(new Person.Phone("1234"));
        phoneList.add(new Person.Phone("5678"));
    }
    Person person = new Person("xuyu", 20, 175.0, true, new Person.Address("南山"), phoneList);
    String s = new Gson().toJson(person);
    {
        Gson gsonGood = new GsonBuilder().registerTypeAdapterFactory(new NoReflectTypeAdapterFactory()).create();
        long startTime = System.currentTimeMillis();
        gsonGood.fromJson(s, Person.class);
        Log.d(TAG, "testTime: take time good :" + (System.currentTimeMillis() - startTime));
    }
    {
        Gson gson = new Gson();
        long startTime = System.currentTimeMillis();
        gson.fromJson(s, Person.class);
        Log.d(TAG, "testTime: take time old  :" + (System.currentTimeMillis() - startTime));
    }
}
```



可以看到我们的优化方案对于大json，解析性能有了明显的优化：

```
2021-11-14 23:13:02.470 12616-12616/com.example.gsonoptimize D/Test: testTime: take time good :1174
2021-11-14 23:13:06.153 12616-12616/com.example.gsonoptimize D/Test: testTime: take time old  :3683
```



### APT低侵入自动化实现

以上我们虽然实现了new来代替反射的优化，但是可以看到我们的实现很繁琐，要针对每一个实体类来写对应的TypeAdapter，那么有没有办法简化如上步骤呢？办法就是通过apt来生成这些代码。

APT的实现步骤我们这里并不会详细介绍，这里只简单介绍一些关键地方。

首先我们定义一个APT要用到的注解在一个专门的模块：

```java
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface GsonOptimize {
}
```

然后创建一个注解处理器的模块并添加如下注解处理器代码：

```java
@AutoService(Processor.class)
public class GsonOptimizeProcessor extends AbstractProcessor {
		···
    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        //得到所有包含该注解的element集合
        Set<? extends Element> elements = roundEnvironment.getElementsAnnotatedWith(GsonOptimize.class);

        for (Element element : elements) {
            //转换为VariableElement，VariableElement为element的子类
            TypeElement typeElement = (TypeElement) element;


            generateTypeAdapter(typeElement);


        }
        generateTypeFactory();
        return true;
    }

    ···

    private void generateTypeFactory() {
        String packageName = "com.example.gsonoptimize_processor";
        ClassName typeAdapterFactory = ClassName.get(packageName, "NoReflectTypeAdapterFactory");

        TypeSpec.Builder typeAdapterBuilder = TypeSpec.classBuilder(typeAdapterFactory)
                .addSuperinterface(ClassName.get(TypeAdapterFactory.class))
                .addMethod(getCreateMethod())
                .addModifiers(Modifier.PUBLIC);

        JavaFile file = JavaFile.builder(packageName, typeAdapterBuilder.build()).build();
        try {
            file.writeTo(processingEnv.getFiler());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void generateTypeAdapter(TypeElement typeElement) {
        String packageName = "com.example.gsonoptimize_processor";
        String typeAdapterClassName = "NoReflect" + typeElement.getSimpleName().toString() + "TypeAdapter";
        mSymbolStringHashMap.put((Symbol.ClassSymbol) typeElement, typeAdapterClassName);

        ClassName typeAdapter = ClassName.get(packageName, typeAdapterClassName);
        ClassName baseTypeAdapter = ClassName.get(TypeAdapter.class);


//        public NoReflectPersonTypeAdapter(Gson gson) {
//            this.gson = gson;
//        }
        MethodSpec constructorMethod = MethodSpec.constructorBuilder()
                .addParameter(ParameterSpec.builder(ClassName.get(Gson.class), "gson").build())
                .addStatement("this.gson = gson").build();

//        @Override
//        public void write(JsonWriter out, T value) {
//
//        }
        ArrayList<ParameterSpec> parameterSpecs = new ArrayList<>();
        parameterSpecs.add(ParameterSpec.builder(ClassName.get(JsonWriter.class), "out").build());
        parameterSpecs.add(ParameterSpec.builder(tTypeName, "value").build());
        MethodSpec writeMethod = MethodSpec.methodBuilder("write")
                .addAnnotation(Override.class)
                .addParameters(parameterSpecs)
                .addModifiers(Modifier.PUBLIC)
                .build();


        TypeSpec.Builder typeAdapterBuilder = TypeSpec.classBuilder(typeAdapter)
                .addTypeVariable(tTypeName)
                .superclass(ParameterizedTypeName.get(baseTypeAdapter, tTypeName))
                .addField(FieldSpec.builder(ClassName.get(Gson.class), "gson").addModifiers(Modifier.PRIVATE).build())
                .addMethod(constructorMethod)
                .addMethod(writeMethod)
                .addMethod(readMethod(typeElement, tTypeName))
                .addModifiers(Modifier.PUBLIC);


        JavaFile file = JavaFile.builder(packageName, typeAdapterBuilder.build()).build();

        try {
            file.writeTo(processingEnv.getFiler());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
  	···

}
```

如上注解处理器会在编译构建的时候，自动生成NoReflectTypeAdapterFactory，以及针对每个添加了GsonOptimize注解的实体类，创建出对应的NoReflectXXXTypeAdapter，这样，我们就可以在解析json的时候，使用如下方式：

```java
new GsonBuilder().registerTypeAdapterFactory(new NoReflectTypeAdapterFactory()).create()
```

以此创建出来的gson会使用new来创建有GsonOptimize注解的实体类