### **Development**  
- Include clear and meaningful comments in your code to improve readability and maintainability.  
- Maximize cohesion by encapsulating related behavior and state within the same class.
- Favor functional programming approaches when working with Java to leverage its modern capabilities.  
- Use the service pattern for modular business logic.
- Apply the repository pattern for data persistence. 
- Use the builder pattern for classes with more than seven attributes.  
- Utilize the DTO (Data Transfer Object) pattern for handling data in request and response layers.  
- Opt for Java records whenever they are suitable to simplify data representation.  
- Prefer immutable objects and data structures to prevent side effects. 
- Ensure immutability by using constructors or creational patterns to instantiate valid objects
- Use constructor-based dependency injection to ensure immutability and improve testability. 
- Avoid returning null; use Optional to handle absent values safely and clearly.  
- Apply SOLID principles to create maintainable and scalable object-oriented designs.
- A class should have only one reason to change, meaning it should only have one job or responsibility.
- Classes should be open for extension but closed for modification.
- Subtypes must be substitutable for their base types without altering the correctness of the program.
- A class should not be forced to implement interfaces it does not use.
- High-level modules should not depend on low-level modules. Both should depend on abstractions.  
- Prefer composition over inheritance to create flexible and reusable code structures.  
- Follow Domain-Driven Design (DDD) principles to align code with business concepts.
- Apply Clean Architecture to separate concerns into layers.
- Each layer of the architecture has a distinct responsibility, ensuring that logic is organized and encapsulated.
- Dependencies flow inward, meaning outer layers depend on inner layers but not vice versa.
- Write Clean Code that prioritizes simplicity and readability.
- Use Bean Validation annotations (e.g., @NotNull, @Valid) for request validation.us on - simplicity, readability, and maintainability in all aspects of your development.
- Business rules must be declared explicitly in our application.
- Write code with testability in mind by designing for clear dependencies and isolated functionality.
- Use comments and metadata to clarify code usage, enforce constraints, and improve maintainability.
- Apply Cognitive-Driven Development (CDD) to measure and control code complexity, using rules to evaluate coupling, conditionals, and extra code blocks.
- Minimize cognitive complexity by simplifying control flow and reducing deeply nested conditionals to improve code readability and maintainability.
- Manage state changes with immutability, defensive programming, and well-defined APIs to ensure reliability and predictability.

### **Testing**  
- Write isolated tests to focus on a single unit of functionality, avoiding dependencies on external systems. 
- Derive test cases pragmatically from known techniques, covering boundary conditions and using property-based testing to ensure high coverage and reliability. 
- Use mocks and stubs appropriately to mock dependencies and isolate the unit under test.  
- Follow the GWT (Given, When, Then) or BDD pattern when testing Resource or Controller classes.  
- Follow the AAA (Arrange, Act, Assert) pattern when testing service classes.  
- Test edge cases: Always include tests for boundary and edge cases to ensure robustness.  
- Write test method names that clearly describe the scenario and expected outcome.  
- Test exceptions and error handling to verify that your code behaves correctly under failure conditions.  
- Use parameterized tests for testing multiple inputs efficiently.  
- Verify interactions: Use Mockito’s `verify` method to check that expected interactions occur.  
- Maintain test independence: Design tests to run independently, without relying on the order of execution.  
- Ensure idempotency in APIs to allow repeated operations without unintended side effects.

### **Documentation**  
- Document APIs: Ensure public APIs are well-documented using tools like Javadoc, Swagger, or TypeDoc.  
- Use expressive variable names to clearly communicate intent within your code.  
- Optimize for readability: Write code that prioritizes readability over cleverness.  

  

