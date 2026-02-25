package net.minecraft.client.model;





public record AdultAndBabyModelPair<T extends Model>(T adultModel, T babyModel) {
   public T getModel(boolean p_397370_) {
      return p_397370_ ? this.babyModel : this.adultModel;
   }
}
