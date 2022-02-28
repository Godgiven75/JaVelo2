package epfl.javelo.projection.data;
import epfl.javelo.Preconditions;
import java.util.StringJoiner;
import static epfl.javelo.projection.data.Attribute.ALL;

/**
 * Enregistrement représentant un ensemble d'attributs OpenStreetMap. Possède un unique attribut : le long bits,
 * qui représente le contenu de l'ensemble au moyen d'un bit par valeur possible; càd que le bit d'index b de cette
 * valeur est 1 si et seulement si l'attribut b est contenu dans l'ensemble.
 */
public record AttributeSet(long bits) {

    public AttributeSet {
        Preconditions.checkArgument((bits << Attribute.values().length) == 0L);
    }

    /**
     * Retourne un ensemble contenant uniquement les attributs passés en argument
     * @param attributes
     * @return un nouvel AttributeSet
     */
    public static AttributeSet of(Attribute... attributes) {
        long nb = 0L;
        for (Attribute a : attributes) {
            long mask = 1L << a.ordinal();
            nb |= mask;
        }
        return new AttributeSet(nb);
    }

    /**
     * Retourne vrai si et seulement si l'ensemble récepteur (this) contient l'attribut donné
     * @param attribute
     * @return vrai si et seulement si l'ensemble récepteur (this) contient l'attribut donné
     */
    public boolean contains(Attribute attribute) {
        return (this.bits << attribute.ordinal()) == 1;
    }

    /**
     * Retourne vrai si et seulement si l'intersection de l'ensemble récepteur (this) avec celui passé en argument (that)
     * n'est pas vide
     * @param that
     * @return vrai si et seulement si l'intersection de l'ensemble récepteur (this) avec celui passé en argument (that)
     * n'est pas vide
     */
    public boolean intersects(AttributeSet that) {
        return (this.bits & that.bits) != 0L;
    }

    /**
     * Redéfinition de toString() afin de retourner une chaîne composée de la représentation textuelle des éléments
     * de l'ensemble entourés d'accolades et séparés par des virgules : les éléments apparaissent dans l'ordre dans
     * lequel ils sont déclarés dans le type énuméré Attribute
     * @return une chaîne composée de la représentation textuelle des éléments de l'ensemble entourés d'accolades ({})
     * et séparés par des virgules
     */
    @Override
    public String toString() {
        StringJoiner j = new StringJoiner(",", "{", "}");
        for (int i = 0; i < 64; i++) {
            if ( ( (bits >> i) % 2 == 1 ) ) {
                j.add(ALL.get(i).key()).add(ALL.get(i).value());
            }
        }
        return j.toString();
    }
}

